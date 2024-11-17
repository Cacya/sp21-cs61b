# Gitlet Design Document

**Name**: Cacya

## Classes and Data Structures

在 Gitlet 中，您可以同时提交多个文件。
每次提交视为整个项目在某个时间点的快照。

每个提交都包含对其之前的提交的某种引用
之前的提交称为父提交
这是一个链表！

HEAD指针->跟踪我们当前在链表中的位置

Gitlet 还有一个妙招：
它不仅可以维护旧版本和新版本的文件，还可以维护不同的版本
Gitlet 允许您保存两个版本，并在它们之间随意切换。
它不再是一个真正的链表了。它更像一棵树。我们将这个东西称为提交树。
按照这个比喻，每个单独的版本都称为树的一个分支。
head pointer是当前分支前面的指针。

关键思路： 设计之初就考虑到持久性。
在选择类和数据结构时，请记住你需要能够从磁盘加载/保存东西。
运行时/内存限制。如果需要一个提交中的信息，
就不应该同时加载所有提交的信息（这会占用太多时间和内存）。
快速检索到小块信息。

关键想法： Give yourself persistence “for free”.
尽量抽象出持久性。
对于每一个需要持久化的对象，编写一个辅助方法将其从文件系统中加载，并编写一个辅助方法将其保存到文件系统中。
在执行 gitlet 命令时，你永远不必担心加载或保存的方式和位置。*
熟悉懒加载和缓存的概念。


未跟踪？ -> 在CWD中，，没有暂存&&（头提交中没有||已准备删除但随后在 Gitlet 不知情的情况下重新创建的文件）



### Repository
Represents a gitlet repository.

#### Fields

1. `public static final File CWD = new File(System.getProperty("user.dir"));`
   CWD，当前文件夹
2. `public static final File GITLET_DIR = join(CWD, ".gitlet");`
   GITLET_DIR文件夹，存放旧文件副本和其他元数据的地方
3. `public static File Index = join(GITLET_DIR, "index");`
   Index文件，记录了哪些文件是暂存的，存储TreeMap<String, String> index，
4. `public static File Remove = join(GITLET_DIR, "removes");`
   Remove文件, 记录了哪些文件是通过rm操作在cwd删除，将要在下一次提交中不出现的
5. `public static File HEAD = join(GITLET_DIR, "HEAD");`
   HEAD文件存储了当前分支的名称
6. `public static final File OBJECT_DIR = join(GITLET_DIR, "objects");`
   OBJECT_DIR文件夹，存放对象。
   每一个Commit对应一个Commit对象,~~每一个文件对应一个Blob对象，~~都放在OBJECTS_DIR下
   对于Commit文件，文件名为commit的id(sha1值），文件内容为commit_obj
7. `public static final File BLOB_DIR = join(GITLET_DIR, "blobsDir");`
    BLOB_DIR文件夹，存放所有的文件副本，真正存文件内容的地方，文件名为sha1值
8. `public static final File BRANCH_DIR = join(GITLET_DIR, "branch");`
    BRANCH_DIR文件夹，文件名为分支名称，文件内容为分支head指向的提交的sha1值
9. `public static final File VERSIONS_DIR = join(GITLET_DIR, "versions");`
    VERSIONS_DIR文件夹，文件名为commit的blobID，文件内容为TreeMap形式的blobs，由文件名映射到sha1
10. `public static TreeMap<String, String> stage = new TreeMap<>();`
    stage图，匹配filename与对应文件的sha1值
11. `public static TreeSet<String> move = new TreeSet<>();`
   move集合，存放要在下一个commit中删除的文件的文件名


   

### Stage
暂存区：.git/index: 文件包含了当前已暂存的文件的结构信息：
1. 文件路径，
2. 文件的 Blob 哈希：Git 使用哈希值来唯一标识每个文件内容
3. 文件的元数据信息：包括文件的权限、大小、最后修改时间等信息，这些元数据用于检测文件是否被修改过。
4. 树结构信息：Git 用一种树形结构来表示项目的目录结构。.git/index 文件保存了这种结构，以便在创建提交时可以正确恢复目录结构。

创建 Blob 对象：当你运行 git add 命令时，Git 会根据文件内容生成一个唯一
的 SHA-1 哈希值，并将该内容作为 Blob 对象存储在 .git/objects 文件夹中。
这些 Blob 对象按照哈希值进行命名和存储。

记录索引信息：Git 将文件的路径、文件的 Blob 哈希值、
文件权限等信息记录在 .git/index 文件中，这个文件就是暂存区的核心记录。
它并不包含文件内容，而是引用了 .git/objects 中的 Blob 对象。

### Main
takes in arguments from the command line 
and calls the corresponding command,
validates the arguments
to ensure that enough arguments were passed in

#### Fields
This class has no fields and hence no associated state: 
it simply validates arguments and defers the execution to the `Repository` class.



### Branch
Maintaining related sequences of commits

#### Fields
1. pointer 
   representing the furthest point of each branch.
2. commits


### Commit
Represents a `gitlet commit` object.
Function:
Saves a snapshot of tracked files in the current commit and staging area 
so they can be restored at a later time, creating a new commit.

it will keep versions of files exactly as they are, 
save and start tracking any files that were staged for addition but weren’t tracked by its parent
and update the version of the file that was staged.

Note: files tracked in the current commit may be untracked


Procedure:
1. 创建提交对象
  执行 git commit 后，Git 会创建一个包含日志消息(log) 、时间戳(timestamp)、
   a mapping of file names to blob references、 父引用和（用于合并）第二个父引用的提交对象。
2. 更新mapping
   先同父，然后对于每个stage的文件，（若不存在，存储这个对象在objects，）更新版本。
3. 提交后暂存区会被清除。
4. 生成自己的sha1值在。。。？
5. 在提交命令之后，新的提交将作为新节点添加到提交树中。
6. 更新当前分支
   刚刚进行的提交成为“当前提交”，头指针（head)现在指向它。前一个头提交是此提交的父提交。
   新的提交对象被创建后，Git 将更新当前分支的 HEAD 指针指向这个新提交，以确保分支历史的连续性。

#### Fields
commits
1. `private String message;` 
    提交的日志消息
2. `private Date date`  
    时间戳
3. `private TreeMap<String, String> blobs = new TreeMap<>()`
   文件版本树，对于提交中的所有文件，存储filename到文件版本（sha1)的映射
4. `private String parentSha1;`  
    父亲的hash值


### GitletException
If fails, print the error message and not change anything else.

#### Fields
No Fields.

### Utils


#### Fields

1. 
2. 
3. Field 2


### Class 4

#### Fields

1.
2.
3. Field 2


## Algorithms

## Persistence

