package gitlet;

import java.io.Serializable;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;


import static java.lang.System.exit;

/** Class to handle our data structures.
 * @author Avi Rabin. */
class Gitlet implements Serializable {
    /** Boolean instantiated. */
    private boolean _instantiated;
    /** Initialize this class. With
     * boolean INSTANTIATED. */
    Gitlet(boolean instantiated) {
        _instantiated = instantiated;
    }
    /** Use TOADD to add a Commit. */
    public void addCommit(Commit toadd) {
        commits.add(toadd);
    }
    /** Use TOADD to add a Branch. */
    public void addBranch(Branch toadd) {
        String name = toadd.getname();
        boolean added = false;
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).getname().equals(name)) {
                branches.set(i, toadd);
                added = true;
            }
        }
        if (!added) {
            branches.add(toadd);
        }
    }
    /** Using TODELETE to delete a Branch.
     * Return the boolean. */
    public boolean deleteBranch(String todelete) {
        boolean deleted = false;
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).getname().equals(todelete)) {
                branches.remove(i);
                branches.trimToSize();
                deleted = true;
            }
        }
        return deleted;
    }
    /** Using ITEM and LOCATION we Serialize our commits. */
    public void serializeitem(Object item, String location) {
        File outfile = new File(location);
        try {
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(outfile));
            out.writeObject(item);
            out.close();
        } catch (IOException excp) {
            exit(0);
        }
    }
    /** Using LOCATION get our commits. */
    @SuppressWarnings("unchecked")
    public void getcommits(String location) {
        File infile = new File(location);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(infile));
            commits = (ArrayList<Commit>) inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            System.out.println(excp);
            exit(0);
        }
    }
    /** Using LOCATION, get the branches. */
    @SuppressWarnings("unchecked")
    public void getBranches(String location) {
        File infile = new File(location);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(infile));
            branches = (ArrayList<Branch>) inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            System.out.println(excp);
            exit(0);
        }
    }
    /** Get staged files from the LOCATION area. */
    @SuppressWarnings("unchecked")
    public void getstage(String location) {
        File inFile = new File(location);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(inFile));
            ArrayList<Tree> tempfiles;
            tempfiles = (ArrayList<Tree>) inp.readObject();
            inp.close();
            stagedfiles.addAll(tempfiles);
        } catch (IOException | ClassNotFoundException excp) {
            File test = new File(".gitlet");
            if (Utils.plainFilenamesIn(test).contains("stagedfiles")) {
                System.out.println(excp);
            }
        }
    }
    /** Return the commits here. */
    public ArrayList<Commit> getcommits() {
        return commits;
    }
    /** Return the Branches here. */
    public ArrayList<Branch> getBranches() {
        return branches;
    }
    /** Add TOSTAGE to the staging area. */
    public void stagefile(File tostage) {
        for (int i = 0; i < stagedfiles.size(); i++) {
            if (stagedfiles.get(i).getname().equals(
                    tostage.getName())) {
                stagedfiles.remove(i);
                stagedfiles.trimToSize();
            }
        }
        stagedfiles.add(new Tree(null, tostage));
    }
    /** Return the staged files. */
    public ArrayList<Tree> getstaged() {
        return stagedfiles;
    }
    /** Return the active Branch. */
    public Branch getactiveBranch() {
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).isactive()) {
                return branches.get(i);
            }
        }
        return null;
    }
    /** Return Tree from the staged files.
     * Given argument BLOBS */
    private Tree listtoTree(ArrayList<Tree> blobs) {
        if (blobs.isEmpty()) {
            return null;
        }
        Tree blob = blobs.remove(0);
        blobs.trimToSize();
        if (!blobs.isEmpty()) {
            blob.combineTrees(blobs);
        }
        return blob;
    }
    /** Return a Tree from the staged files. */
    public Tree stagedtoTree() {
        ArrayList<Tree> tempstage = new ArrayList<>();
        tempstage.addAll(stagedfiles);
        if (tempstage.isEmpty()) {
            return null;
        }
        Tree returned = listtoTree(stagedfiles);
        stagedfiles.addAll(tempstage);
        return returned;
    }
    /**Add to the staged files any files from the Commit.
     * This is just for the purpose of Committing.  Don't forget to
     * clear the staged files afterwards. Takes in the arguments
     * NEWCOMMIT and PREVCOMMIT to use.*/
    public void inherittostaged(Tree newCommit, Tree prevCommit) {
        ArrayList<Tree> inherits = new ArrayList<>();
        Tree x = prevCommit;
        while (x != null) {
            if (!newCommit.hasfile(x.getname())) {
                inherits.add(x.nochild());
            }
            x = x.getchild();
        }
        newCommit.combineTrees(inherits);
    }
    /** Clear the staged files. */
    public void clearstaged() {
        stagedfiles.clear();
    }
    /** Return an ArrayList of untracked files. */
    public ArrayList<File> untracked() {
        return untrackedfiles;
    }
    /** ADD to the untracked files. */
    public void adduntracked(File add) {
        for (int i = 0; i < untrackedfiles.size(); i++) {
            if (untrackedfiles.get(i).toString().equals(
                    add.getName())) {
                untrackedfiles.remove(i);
            }
        }
        untrackedfiles.trimToSize();
        untrackedfiles.add(add);
    }
    /** REMOVE from the untracked files. */
    public void removeuntracked(File remove) {
        for (int i = 0; i < untrackedfiles.size(); i++) {
            if (untrackedfiles.get(i).getName().equals(remove.getName())) {
                untrackedfiles.remove(i);
                untrackedfiles.trimToSize();
            }
        }
    }
    /**Get untracked files from a LOCATION. */
    @SuppressWarnings("unchecked")
    public void getuntracked(String location) {
        File inFile = new File(location);
        try {
            ObjectInputStream inp =
                    new ObjectInputStream(new FileInputStream(inFile));
            ArrayList<File> tempfiles;
            tempfiles = (ArrayList<File>) inp.readObject();
            inp.close();
            untrackedfiles.addAll(tempfiles);
        } catch (IOException | ClassNotFoundException excp) {
            File test = new File(".gitlet");
            if (Utils.plainFilenamesIn(test).contains("untrackedfiles")) {
                System.out.println(excp);
            }
        }
    }
    /**Return a Tree without a given file.
     * Takes in the argument BLOATED.*/
    public Tree untrackedtrimmed(Tree bloated) {
        ArrayList<Tree> untrack = new ArrayList<>();
        Tree curr = bloated;
        while (curr != null) {
            untrack.add(curr.nochild());
            curr = curr.getchild();
        }
        for (int i = 0; i < untrackedfiles.size(); i++) {
            for (int y = 0; y < untrack.size(); y++) {
                if (untrackedfiles.get(i).toString().equals(
                        untrack.get(y).getname())) {
                    untrack.remove(y);
                }
            }
        }
        untrack.trimToSize();
        return listtoTree(untrack);
    }
    /**Remove a file from the staged files.
     * Takes in the file REMOVE. */
    public void removestaged(File remove) {
        for (int i = 0; i < stagedfiles.size(); i++) {
            if (stagedfiles.get(i).getname().equals(
                    remove.getName())) {
                stagedfiles.remove(i);
            }
        }
        stagedfiles.trimToSize();
    }
    /** Print all Branches. */
    public void printBranches() {
        ArrayList<String> names = new ArrayList<>();
        String special = "null";
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).isactive()) {
                special = branches.get(i).getname();
            }
            names.add(branches.get(i).getname());

        }
        Object[] toprint = names.toArray();
        Arrays.sort(toprint);
        System.out.println("=== Branches ===");
        for (int i = 0; i < toprint.length; i++) {
            if (toprint[i].equals(special)) {
                System.out.println("*" + toprint[i]);
            } else {
                System.out.println(toprint[i]);
            }
        }
        System.out.println("");
    }
    /** Print all staged files. */
    public void printstaged() {
        System.out.println("=== Staged Files ===");
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < stagedfiles.size(); i++) {
            names.add(stagedfiles.get(i).getname());
        }
        Object[] toprint = names.toArray();
        Arrays.sort(toprint);
        for (int i = 0; i < toprint.length; i++) {
            System.out.println(toprint[i]);
        }
        System.out.println("");
    }
    /** Print all untracked files. */
    public void printuntracked() {
        System.out.println("=== Removed Files ===");
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < untrackedfiles.size(); i++) {
            names.add(untrackedfiles.get(i).getName());
        }
        Object[] toprint = names.toArray();
        Arrays.sort(toprint);
        for (int i = 0; i < toprint.length; i++) {
            System.out.println(toprint[i]);
        }
        System.out.println("");
    }
    /**Get the Branch with the given name.
     * Uses argument BRANCHNAME. Returns a Branch. */
    public Branch getBranch(String branchname) {
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).getname().equals(branchname)) {
                return branches.get(i);
            }
        }
        System.out.println("A branch with that name does not exist.");
        exit(0);
        return null;
    }
    /** Clear the untracked files. */
    public void clearuntracked() {
        untrackedfiles.clear();
    }
    /** A list of our commits. */
    private ArrayList<Commit> commits = new ArrayList<>();
    /** A list of pointers. */
    private ArrayList<Branch> branches = new ArrayList<>();
    /** The staging area of files. */
    private ArrayList<Tree> stagedfiles = new ArrayList<>();
    /** The untracked files. */
    private ArrayList<File> untrackedfiles = new ArrayList<>();
}
