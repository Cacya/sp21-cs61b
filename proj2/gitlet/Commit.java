package gitlet;

// TODO: any imports you need here

import com.sun.source.tree.Tree;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.join;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Cacya
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** a mapping of file names to blob references */
    private String blobID;
    private String message;
    private String date;
    private String parentSha1;
    private String parentSha2;

    /* TODO: fill in the rest of this class. */
    public Commit(String mes) {
        this(mes, "");
    }

    public Commit(String mes, String sha2) {
        this.message = mes;
        this.parentSha2 = sha2;
        if (Repository.HEAD.exists()) {
            this.parentSha1 = Repository.getCurCmtSha1();
        } else {
            this.parentSha1 = "";
        }
        this.date = dateToTimeStamp(new Date(0));
        setBlobs();
    }

    private void setBlobs() {
        TreeMap<String, String> blobs;
        if (this.parentSha1 == "") {
            blobs = new TreeMap<>();
        } else {
            blobs = Repository.getCommit(parentSha1).getBlobs();
        }

        // Should also guarantee Remove file exists
        TreeSet<String> moves = Utils.readObject(Repository.Remove, TreeSet.class);
        for (String k: moves) {
            blobs.remove(k);
        }

        // Maybe should guarantee Index file exists
        TreeMap<String, String> stage = Utils.readObject(Repository.Index, TreeMap.class);
        for (String k: stage.keySet()) {
            blobs.put(k, stage.get(k));
        }

        // Save blobs tree object
        blobID = Utils.sha1(Utils.serialize(blobs));
        Utils.writeObject(Utils.join(Repository.VERSIONS_DIR, blobID), blobs);
    }

    public TreeMap<String, String> getBlobs() {
        return Utils.readObject(Utils.join(Repository.VERSIONS_DIR, blobID), TreeMap.class);
    }

    public String getParentSha1() {
        return parentSha1;
    }

    public String getParentSha2() {
        return parentSha2;
    }

    public String getDate() {
        return date;
    }

    private static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }

    public String getMessage() {
        return message;
    }
}
