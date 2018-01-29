package gitlet;
import java.util.Date;
import java.io.Serializable;
import java.util.Formatter;

/** Commit class.
 * @author Avi Rabin. */
class Commit implements Serializable {

    /** Time. */
    private Long _time;
    /** Log message. */
    private String _logmessage;
    /** Parent commit. */
    private Commit _parent;
    /** Our blob of contents. */
    private Tree _contents;
    /** Our sha1 code. */
    private String _sha;
    /** Merge message. */
    private String _mm;
    /** The two commits merged into this. */
    private String _twomerges;
    /** Our shortened sha. */
    private String _shortsha;
    /** Create a new commit with a message, PARENT, and Tree pointer.
     * Uses a LOGMESSAGE and CONTENTS. */
    Commit(String logmessage, Commit parent, Tree contents) {
        Date currdate = new Date();
        _time = currdate.getTime();
        _logmessage = logmessage;
        if (parent == null) {
            _parent = null;
        } else {
            _parent = parent;
        }
        _contents = contents;
        _sha = converttosha();
        _mm = null;
        _twomerges = null;
        _shortsha = _sha.substring(0, 6);
    }
    /** Get the initial time. */
    public void inittime() {
        _time = new Date(0).getTime();
    }
    /** Return a string sha. */
    public String converttosha() {
        String temptime = new Date(_time).toInstant().toString();
        String tempparent;
        if (_parent == null) {
            tempparent = "null";
        } else {
            tempparent = _parent.toString();
        }
        String tempcontents;
        if (_contents == null) {
            tempcontents = "null";
        } else {

            tempcontents = _contents.treesha1();
        }
        return Utils.sha1(temptime, _logmessage, tempparent, tempcontents);
    }
    /** Return the blobs of this commit. */
    public Tree getblobs() {
        return _contents;
    }
    /** Return this commit's parent commit. */
    public Commit getparent() {
        return _parent;
    }
    /** Return this commit's time. */
    public String getdate() {
        Date c = new Date(_time);
        StringBuilder buff = new StringBuilder();
        Formatter format = new Formatter(buff);
        format.format("%ta %tb %te %tT %tY %tz", c, c, c, c, c, c);
        return "Date: " + buff.toString();
    }
    /** Return the commit's message. */
    public String getmessage() {
        return _logmessage;
    }
    /** Return this Tree's sha1-code. */
    public String getsha() {
        return _sha;
    }
    /** Return the commit's shortened sha. */
    public String getshortsha() {
        return _shortsha;
    }
    /** Set a merge MESSAGE. */
    public void setmergemessage(String message) {
        _mm = message;
    }
    /** Return the merge message. */
    public String getmm() {
        return _mm;
    }
    /** Set the two merges. Uses MERGE1 and MERGE2 */
    public void settwomerges(String merge1, String merge2) {
        _twomerges = merge1.substring(0, 7)
                + " " + merge2.substring(0, 7);
    }
    /** Return the two merges. */
    public String getmerges() {
        return _twomerges;
    }
}
