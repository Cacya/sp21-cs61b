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
    private String parent_sha1;
    private String parent_sha2;

    /* TODO: fill in the rest of this class. */
    public Commit(String mes) {
        this.message = mes;
        if (Repository.HEAD.exists()) {
            this.parent_sha1 = Repository.getCurCmtSha1();
        } else {
            this.parent_sha1 = "";
        }
        this.parent_sha2 = "";
        this.date = dateToTimeStamp(new Date(0));
        setBlobs();
    }

    public Commit(String mes, String sha2) {
        this.message = mes;
        if (Repository.HEAD.exists()) {
            this.parent_sha1 = Repository.getCurCmtSha1();
        } else {
            this.parent_sha1 = "";
        }
        this.parent_sha2 = "";
        this.date = dateToTimeStamp(new Date(0));
        setBlobs();
    }

    private void setBlobs() {
        TreeMap<String, String> blobs;
        if (this.parent_sha1 == "") {
            blobs = new TreeMap<>();
        } else {
            blobs = Repository.getCommit(parent_sha1).getBlobs();
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

    public String getParent_sha1() {
        return parent_sha1;
    }

    public String getParent_sha2() {
        return parent_sha2;
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

    public void setParent_sha2(String sha2) {
        this.parent_sha2 = sha2;
    }
}
