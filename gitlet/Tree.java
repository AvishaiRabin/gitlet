package gitlet;
import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import static java.lang.System.exit;

/** Parent class for our commits' Trees.
 * @author Avi Rabin */
class Tree implements Serializable {
    /** This Tree's child. */
    private Tree _child;
    /** This Tree's contents. */
    private File _blobcontents;
    /** Our contents, read by utils.readcontents. */
    private byte[] _readcontents;
    /** This file's name. */
    private String _name;
    /**Initialize a Tree.  A Tree should point to a blob or another Tree.
     * It should have a Tree CHILD and file CONTENTS. */
    Tree(Tree child, File contents) {
        _child = child;
        _name = "" + contents.getName();
        _blobcontents = copyfile(contents);
        _readcontents = Utils.readContents(contents);
    }

    /**
     * Return this file's name.
     */
    public String getname() {
        return _name;
    }

    /**
     * Return the file.
     */
    public File getblob() {
        return _blobcontents;
    }

    /** Return the child of this Tree. */
    public Tree getchild() {
        return _child;
    }

    /** Return the boolean and file POSSIBLE
     * whether this file is contained. */
    public boolean containsfile(File possible) {
        if (getname().equals(possible.getName())) {
            return true;
        } else {
            if (_child == null) {
                return false;
            } else {
                return _child.containsfile(possible);
            }
        }
    }

    /** Return the boolean given the file POSSIBLE
     * whether its exactly here. */
    public boolean hasexact(File possible) {
        String a = Utils.sha1(readfiles());
        String b;
        if (possible.getName().substring(0, 7).equals((".gitlet"))) {
            b = Utils.sha1(Utils.readContents(possible));
        } else {
            b = possible.getName();
        }
        if (a.equals(b)) {
            return true;
        } else {
            if (_child == null) {
                return false;
            } else {
                return _child.hasexact(possible);
            }
        }
    }

    /** Return the read contents of this file. */
    public byte[] readfiles() {
        return _readcontents;
    }

    /** Return the sha1 of this Tree. */
    public String treesha1() {
        if (_child == null) {
            return Utils.sha1(readfiles());
        }
        return Utils.sha1(readfiles() + _child.treesha1());
    }
    /** Return the sha1code of this blob. */
    public String filesha1() {
        return Utils.sha1(readfiles());
    }
    /** Use SOURCE to return a copy of a file. */
    private static File copyfile(File source) {
        try {
            String sha = Utils.sha1(Utils.readContents(source));
            File path = new File(".gitlet");
            File dest = new File(path.toPath() + "\\" + sha);
            if (!dest.isFile()) {
                Files.copy(source.toPath(), dest.toPath());
            }
            return dest;
        } catch (IOException excp) {
            System.out.println(excp);
            exit(0);
        }
        return null;
    }
    /** Return this Tree as a singular entity - that is,
     * without any children. */
    public Tree nochild() {
        Tree childless = new Tree(null, _blobcontents);
        childless.newname(_name);
        return childless;
    }
    /** Use NAME to change this Tree's name. */
    public void newname(String name) {
        _name = name;
    }
    /** Use ADD to combine two Trees. */
    public void combineTrees(ArrayList<Tree> add) {
        Tree curr = this;
        while (curr.getchild() != null) {
            curr = curr.getchild();
        }
        for (int i = 0; i < add.size(); i++) {
            curr._child = add.get(i);
            curr = curr.getchild();
        }
    }
    /** Return a list of the names of all files that are in
     * the current Tree and have been modified from the Tree SPLIT. */
    public ArrayList<String> modified(Tree split) {
        Tree curr = this;
        ArrayList<String> modded = new ArrayList<>();
        while (curr != null) {
            if (!split.hasexact(curr.getblob())) {
                modded.add(curr.getname());
            }
            curr = curr.getchild();
        }
        return modded;
    }
    /** Return the child in this Tree with the given NAME here. */
    public Tree getchild(String name) {
        if (getname().equals(name)) {
            return this;
        }
        Tree curr = getchild();
        while (!curr.getname().equals(name)) {
            curr = curr.getchild();
        }
        return curr;
    }
    /**Return a list of the file names present in this Tree
     * and not in the given parameter SPLIT Tree. */
    public ArrayList<String> splitmissing(Tree split) {
        ArrayList<String> missing = new ArrayList<>();
        if (!split.hasfile(getname())) {
            missing.add(getname());
        }
        Tree curr = getchild();
        while (curr != null) {
            if (!split.hasfile(curr.getname())) {
                missing.add(curr.getname());
            }
            curr = curr.getchild();
        }
        return missing;
    }
    /** Return boolean, String NAME for if it has a file. */
    public boolean hasfile(String name) {
        Tree curr = this;
        if (curr.getname().equals(name)) {
            return true;
        } else if (curr.getchild() == null) {
            return false;
        } else {
            return curr.getchild().hasfile(name);
        }
    }
    /**Return whether a given file is the same in this Tree
     * as it is in another. Essentially the opposite of modified.
     * Takes in the Tree GIVEN and String F for its use.*/
    public boolean unmodified(Tree given, String f) {
        return shaname(f).equals(given.shaname(f));
    }
    /** Return the shacode of the file with this String NAME
     * in this Tree. */
    public String shaname(String name) {
        if (getname().equals(name)) {
            return filesha1();
        }
        return getchild().shaname(name);
    }
}
