package gitlet;

import java.awt.desktop.SystemEventListener;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Cacya
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                checkArgsLength(args.length, 1);
                Repository.init();
                break;
            case "add":
                check(args.length, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length < 2 || args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                }
                check(args.length, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                check(args.length, 2);
                Repository.remove(args[1]);
                break;
            case "log":
                check(args.length, 1);
                Repository.log();
                break;
            case "global-log":
                check(args.length, 1);
                Repository.globalLog();
                break;
            case "find":
                check(args.length, 2);
                Repository.find(args[1]);
                break;
            case "status":
                check(args.length, 1);
                Repository.getStatus();
                break;
            case "checkout":
                checkRepo();
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else if (args.length == 3 && args[1].equals("--")) {
                    Repository.checkoutFile(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkoutId(args[1], args[3]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "branch":
                check(args.length, 2);
                Repository.createBranch(args[1]);
                break;
            case "rm-branch":
                check(args.length, 2);
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                check(args.length, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                check(args.length, 2);
                Repository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    public static void checkArgsLength(int act, int except) {
        if (act != except) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void check(int act, int except) {
        checkArgsLength(act, except);
        checkRepo();
    }

    public static void checkRepo() {
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
