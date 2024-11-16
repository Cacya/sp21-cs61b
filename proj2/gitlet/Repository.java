package gitlet;

import java.util.*;
import java.io.File;
import java.util.TreeMap;
import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Cacya
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The File HEAD */
    public static File HEAD = join(GITLET_DIR, "HEAD");
    /** The stage file */
    public static File Index = join(GITLET_DIR, "index");
    /** The file which records the files to be removed in the next commit. */
    public static File Remove = join(GITLET_DIR, "remove");
    /** The directory where all the objects of the repository are stored. */
    public static final File OBJECT_DIR = join(GITLET_DIR, "object");
    /** The directory where all blob files of the commit are stored. */
    public static final File BLOB_DIR = join(GITLET_DIR, "blobDir");
    /** The branch reference directory. */
    public static final File BRANCH_DIR = join(GITLET_DIR, "branch");
    /** The blobs treemap directory. */
    public static final File VERSIONS_DIR = join(GITLET_DIR, "versions");
    /** The map of stage. */
    public static TreeMap<String, String> stage = new TreeMap<>();
    /** The set of remove. */
    public static TreeSet<String> move = new TreeSet<>();

    /** The `init` command of gitlet.
     * Which Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit, which contains no files and has the commit message "initial commit".
     * It will have a branch - master, which initially points to this initial commit, and master will be the current branch.
     * */
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        OBJECT_DIR.mkdir();
        BRANCH_DIR.mkdir();
        BLOB_DIR.mkdir();
        VERSIONS_DIR.mkdir();
        setStage();

        Commit init_cmt = new Commit("initial commit");
        setCmtObject(init_cmt);
        setBranch("master", getSha1(init_cmt));
    }

    private static void setCmtObject(Commit cmt) {
        String sha1 = getSha1(cmt);
        Utils.writeObject(Utils.join(OBJECT_DIR, sha1), cmt);
    }

    private static String getSha1(Commit cmt) {
        return Utils.sha1(Utils.serialize(cmt));
    }

    private static void setBranch(String sha1) {
        setBranch(Utils.readContentsAsString(HEAD), sha1);
    }

    private static void setBranch(String branch_name, String sha1) {
        File b = Utils.join(BRANCH_DIR, branch_name);
        Utils.writeContents(b, sha1);
        Utils.writeContents(HEAD, branch_name);
    }

    private static void setStage() {
        Utils.writeObject(Index, stage);
        Utils.writeObject(Remove, move);
    }

    // Get the sha1 value of the head commit of a branch named branch_name.
    private static String getCmtSha1(String branch_name) {
        return Utils.readContentsAsString(Utils.join(BRANCH_DIR, branch_name));
    }

    // Get the sha1 value of current Commit by the information of HEAD.
    public static String getCurCmtSha1() {
        return getCmtSha1(Utils.readContentsAsString(HEAD));
    }

    // Get the Commit object by its Sha1
    public static Commit getCommit(String my_sha1) {
        return Utils.readObject(Utils.join(OBJECT_DIR, my_sha1), Commit.class);
    }

    // Get the sha1 value of file in the current commit. If the file doesn't exist, return null.
    private static String getCurFileSha1(String filename) {
        Commit cur_cmt = getCommit(getCurCmtSha1());
        TreeMap<String, String> blobs = cur_cmt.getBlobs();
        return blobs.get(filename);
    }

    /**  Adds a copy of the file as it currently exists to the staging area
     *   Staging an already-staged file overwrites the previous entry in the staging area with the new contents.
     *   If the current working version of the file is identical to the version in the current commit,
     *   do not stage it to be added, and remove it from the staging area if it is already there
     */
    public static void add(String filename) {
        stage = Utils.readObject(Index, TreeMap.class);

        File cf = Utils.join(CWD, filename);
        if (!cf.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        String cf_contents = Utils.readContentsAsString(cf);
        String cf_sha1 = Utils.sha1(cf_contents);
        String hf_sha1 = getCurFileSha1(filename);
        if (cf_sha1.equals(hf_sha1)) {
            if (stage.containsKey(filename)) {
                stage.remove(filename);
            }
        } else {
            stage.put(filename, cf_sha1);
            Utils.writeContents(Utils.join(BLOB_DIR, cf_sha1), cf_contents);
        }
        Utils.writeObject(Index, stage);
    }

    /** A commit will save and start tracking any files
     *  that were staged for addition but weren't tracked by its parent. */
    public static void commit(String message) {
        stage = Utils.readObject(Index, TreeMap.class);
        move = Utils.readObject(Remove, TreeSet.class);
        if (stage.isEmpty() && move.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit cmt = new Commit(message);
        setCmtObject(cmt);
        setBranch(getSha1(cmt));

        stage = new TreeMap<>();
        move = new TreeSet<>();
        setStage();
    }


    public static void remove(String filename) {
        stage = Utils.readObject(Repository.Index, TreeMap.class);
        TreeMap<String, String> blobs = getCommit(getCurCmtSha1()).getBlobs();
        if (!stage.containsKey(filename) && !blobs.containsKey(filename)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (stage.containsKey(filename)) {
            stage.remove(filename);
        }
        if (blobs.containsKey(filename)) {
            move = Utils.readObject(Remove, TreeSet.class);
            move.add(filename);
            Utils.restrictedDelete(Utils.join(CWD, filename));
        }
        setStage();
    }

    private static String getParent_sha1(String sha1) {
        return getCommit(sha1).getParent_sha1();
    }

    private static void printSingleLog(String sha1, Commit cmt) {
        System.out.println("===");
        System.out.println("commit " + sha1);
        if (cmt.getParent_sha2() != "") {
            System.out.println(String.format("Merge: %s %s", cmt.getParent_sha1().substring(0, 7),
                    cmt.getParent_sha2().substring(0, 7)));
        }
        System.out.println("Date: " + cmt.getDate());
        System.out.println(cmt.getMessage());
        System.out.println();
    }

    public static void log() {
        String sha1 = getCurCmtSha1();
        while (sha1 != "") {
            Commit cmt = getCommit(sha1);
            printSingleLog(sha1, cmt);
            sha1 = cmt.getParent_sha1();
        }
    }

    public static void globalLog() {
        List<String> all_cmt_sha1 = Utils.plainFilenamesIn(OBJECT_DIR);
        for (String cmt_sha1: all_cmt_sha1) {
            Commit cmt = getCommit(cmt_sha1);
            printSingleLog(cmt_sha1, cmt);
        }
    }

    public static void find(String message) {
        List<String> all_cmt_sha1 = Utils.plainFilenamesIn(OBJECT_DIR);
        boolean flag = false;
        for (String cmt_sha1: all_cmt_sha1) {
            Commit cmt = getCommit(cmt_sha1);
            if (cmt.getMessage().equals(message)) {
                System.out.println(cmt_sha1);
                flag = true;
            }
        }
        if (!flag) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist, and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal. */
    public static void getStatus() {
        /* branch */
        System.out.println("=== Branches ===");
        String cur_branch = Utils.readContentsAsString(HEAD);
        List<String> all_branch_name = Utils.plainFilenamesIn(BRANCH_DIR);
        for (String b: all_branch_name) {
            if (b.equals(cur_branch)) {
                System.out.print("*");
            }
            System.out.println(b);
        }
        System.out.println();

        /* Staged Files */
        System.out.println("=== Staged Files ===");
        stage = Utils.readObject(Index, TreeMap.class);
        for (String key: stage.keySet()) {
            System.out.println(key);
        }
        System.out.println();

        /* Removed Files */
        System.out.println("=== Removed Files ===");
        move = Utils.readObject(Remove, TreeSet.class);
        for (String item: move) {
            System.out.println(item);
        }
        System.out.println();

        /* Modifications Not Staged For Commit */
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> cwd_filename = Utils.plainFilenamesIn(CWD);
        TreeMap<String, String> cur_blobs = getCommit(getCurCmtSha1()).getBlobs();
        for (String filename: cwd_filename) {
            String this_sha1 = Utils.sha1(Utils.readContentsAsString(Utils.join(CWD, filename)));
            if (cur_blobs.containsKey(filename) && !cur_blobs.get(filename).equals(this_sha1) && !stage.containsKey(filename)) {
                System.out.println(filename);
            } else if (stage.containsKey(filename) && !stage.get(filename).equals(this_sha1)) {
                System.out.println(filename);
            }
        }
        for (String filename: stage.keySet()) {
            if (!cwd_filename.contains(filename)) {
                System.out.println(filename);
            }
        }
        for (String filename: cur_blobs.keySet()) {
            if (!cwd_filename.contains(filename) && !stage.containsKey(filename) && !move.contains(filename)) {
                System.out.println(filename);
            }
        }
        System.out.println();

        /* Untracked Files */
        System.out.println("=== Untracked Files ===");
        for (String filename: cwd_filename) {
            if (!stage.containsKey(filename) && (!cur_blobs.containsKey(filename) || (move.contains(filename) && cwd_filename.contains(filename)))) {
                System.out.println(filename);
            }
        }
        System.out.println();
    }


    /** java gitlet.Main checkout -- [file name]
     * java gitlet.Main checkout [commit id] -- [file name]
     * java gitlet.Main checkout [branch name]
     */
    public static void checkoutFile(String filename) {
        String cur_sha1 = getCurCmtSha1();
        checkoutId(cur_sha1, filename);
    }

    public static void checkoutId(String id, String filename) {
        if (id.length() < 40) {
            List<String> lst = plainFilenamesIn(OBJECT_DIR);
            int len = id.length();
            for (String lid: lst) {
                if (lid.substring(0, len).equals(id)) {
                    id = lid;
                    break;
                }
            }
        }
        if (!Utils.join(OBJECT_DIR, id).exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        TreeMap<String, String> blobs = getCommit(id).getBlobs();
        if (!blobs.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File cmt_file = Utils.join(BLOB_DIR, blobs.get(filename));
        Utils.writeContents(Utils.join(CWD, filename), Utils.readContents(cmt_file));
    }

    public static void checkoutBranch(String branch_name) {
        if (!branchExists(branch_name)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (branch_name.equals(Utils.readContentsAsString(HEAD))) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        checkSha1(getCmtSha1(branch_name));
        Utils.writeContents(HEAD, branch_name);
    }

    private static void checkSha1(String sha1) {
        List<String> cwd_filename = Utils.plainFilenamesIn(CWD);
        TreeMap<String, String> blobs = getCommit(sha1).getBlobs();
        TreeMap<String, String> cur_blobs = getCommit(getCurCmtSha1()).getBlobs();
        for (String filename: blobs.keySet()) {
            if (cwd_filename.contains(filename) && !stage.containsKey(filename) && (!cur_blobs.containsKey(filename) || move.contains(filename))) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
            File cmt_file = Utils.join(BLOB_DIR, blobs.get(filename));
            Utils.writeContents(Utils.join(CWD, filename), Utils.readContents(cmt_file));
        }

        for (String filename: cur_blobs.keySet()) {
            if (cwd_filename.contains(filename) && !blobs.containsKey(filename)) {
                Utils.restrictedDelete(filename);
            }
        }

        stage = new TreeMap<>();
        move = new TreeSet<>();
        setStage();
    }

    public static void createBranch(String branch_name) {
        if (branchExists(branch_name)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        Utils.writeContents(Utils.join(BRANCH_DIR, branch_name), getCurCmtSha1());
    }

    private static boolean branchExists(String branch_name) {
        return  Utils.join(BRANCH_DIR, branch_name).exists() && !Utils.readContentsAsString(Utils.join(BRANCH_DIR, branch_name)).equals("null");
    }

    public static void removeBranch(String branch_name) {
        String this_branch = Utils.readContentsAsString(HEAD);
        if (!branchExists(branch_name)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (branch_name.equals(this_branch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        Utils.writeContents(Utils.join(BRANCH_DIR, branch_name), "null");
    }

    public static void reset(String cmt_id) {
        if (!Utils.join(OBJECT_DIR, cmt_id).exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        checkSha1(cmt_id);
        Utils.writeContents(Utils.join(BRANCH_DIR, Utils.readContentsAsString(HEAD)), cmt_id);
    }

    private static TreeMap<String, Integer> add_map(String sha1, TreeMap<String, Integer> map, int dist) {
        map.put(sha1, dist);
        while (!sha1.equals("")) {
            dist += 1;
            Commit cmt = getCommit(sha1);
            if (!cmt.getParent_sha2().equals("")) {
                add_map(cmt.getParent_sha2(), map, dist);
            }
            sha1 = cmt.getParent_sha1();
            map.put(sha1, dist);
        }
        return map;
    }

    private static String find_split(String sha1, String sha2) {
        TreeMap<String, Integer> map1 = add_map(sha1, new TreeMap<String, Integer>(), 0);
        TreeMap<String, Integer> map2 = add_map(sha2, new TreeMap<String, Integer>(), 0);
        String min_id = "";
        int min_dist = Integer.MAX_VALUE;
        for (String id: map1.keySet()) {
            if (map2.containsKey(id)) {
                int dist2 = map2.get(id);
                if (dist2 < min_dist) {
                    min_id = id;
                    min_dist = dist2;
                }
            }
        }
        return min_id;
    }

    private static boolean need_merge(String filename, TreeMap<String, String> cur_blobs, TreeMap<String, String> br_blobs, TreeMap<String, String> split_blobs) {
        if (!split_blobs.containsKey(filename)) {
            if (!cur_blobs.containsKey(filename) && br_blobs.containsKey(filename)) {
                return true;
            } else if (cur_blobs.containsKey(filename) && !br_blobs.containsKey(filename)) {
                return true;
            } else if (!cur_blobs.containsKey(filename) && !br_blobs.containsKey(filename)) {
                return false;
            } else if (!cur_blobs.get(filename).equals(br_blobs.get(filename))) {
                return true;
            }
        } else {
            String file_sha1 = split_blobs.get(filename);
            if (!cur_blobs.containsKey(filename) && !br_blobs.containsKey(filename)) {
                return false;
            } else if (!cur_blobs.containsKey(filename)) {
                if (!br_blobs.get(filename).equals(file_sha1)) {
                    return true;
                }
            } else if (!br_blobs.containsKey(filename)) {
                if (!cur_blobs.get(filename).equals(file_sha1)) {
                    return true;
                }
            } else {
                String br_file_id = br_blobs.get(filename);
                String cur_file_id = cur_blobs.get(filename);
                if (!br_file_id.equals(file_sha1) && !br_file_id.equals(cur_file_id) && !cur_file_id.equals(file_sha1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void merge_conflict(String filename, String cur_sha1, String br_sha1) {
        String first = "<<<<<<< HEAD\n";
        if (cur_sha1 != null) {
            first += Utils.readContentsAsString(Utils.join(BLOB_DIR, cur_sha1));
        }
        first += "=======\n";
        if (br_sha1 != null) {
            first += Utils.readContentsAsString(Utils.join(BLOB_DIR, br_sha1));
        }
        first += ">>>>>>>\n";
        String sha1 = Utils.sha1(first);
        Utils.writeContents(Utils.join(CWD, filename), first);
        stage.put(filename, sha1);
    }

    public static void merge(String branch_name) {
        stage = Utils.readObject(Index, TreeMap.class);
        move = Utils.readObject(Remove, TreeSet.class);
        if (!stage.isEmpty() || !move.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        } else if (!branchExists(branch_name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (branch_name.equals(Utils.readContentsAsString(HEAD))) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        String cur_sha1 = getCurCmtSha1();
        String br_sha1 = getCmtSha1(branch_name);
        String split_sha1 = find_split(cur_sha1, br_sha1);
        if (split_sha1.equals(br_sha1)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (split_sha1.equals(cur_sha1)) {
            checkoutBranch(branch_name);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        boolean flag = false;
        TreeMap<String, String> cur_blobs = getCommit(cur_sha1).getBlobs();
        TreeMap<String, String> br_blobs = getCommit(br_sha1).getBlobs();
        TreeMap<String, String> split_blobs = getCommit(split_sha1).getBlobs();
        List<String> cwd_file = plainFilenamesIn(CWD);
        for (String filename: split_blobs.keySet()) {
            String file_sha1 = split_blobs.get(filename);
            if (br_blobs.containsKey(filename) && cur_blobs.containsKey(filename)
                    && !br_blobs.get(filename).equals(file_sha1) && cur_blobs.get(filename).equals(file_sha1)) {
                checkoutId(br_sha1, filename);
                stage.put(filename, br_blobs.get(filename));
            } else if (cur_blobs.containsKey(filename) && cur_blobs.get(filename).equals(file_sha1) && !br_blobs.containsKey(filename)) {
                remove(filename);
            } else if (need_merge(filename, cur_blobs, br_blobs, split_blobs)) {
                merge_conflict(filename, cur_blobs.get(filename), br_blobs.get(filename));
                flag = true;
            }
        }

        for (String filename: br_blobs.keySet()) {
            if (!cur_blobs.containsKey(filename) && !split_blobs.containsKey(filename)) {
                if (cwd_file.contains(filename)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    return;
                }
                checkoutId(br_sha1, filename);
                stage.put(filename, br_blobs.get(filename));
            } else if (!split_blobs.containsKey(filename) && need_merge(filename, cur_blobs, br_blobs, split_blobs)) {
                merge_conflict(filename, cur_blobs.get(filename), br_blobs.get(filename));
                flag = true;
            }
        }
        Utils.writeObject(Index, stage);
        String message = String.format("Merged %s into %s.", branch_name, Utils.readContentsAsString(HEAD));
        commit(message);
        Commit cmt = getCommit(getCurCmtSha1());
        cmt.setParent_sha2(br_sha1);
        if (flag) {
            System.out.println("Encountered a merge conflict.");
        }
    }
}
