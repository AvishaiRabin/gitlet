package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileOutputStream;

import static java.lang.System.exit;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Avi Rabin
 */
public class Main {
    /** Our working directory. */
    private static String workdir = System.getProperty("user.dir");
    /** Usage: java Gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            generalerrors("No Arguments");
        } else if (!_arguments.containsKey(args[0])) {
            generalerrors("Command doesn't exist");
        } else {
            Integer todo = _arguments.get(args[0]);
            switch (todo) {
            case 1: doinit(args);
                break;
            case 2: doadd(args);
                break;
            case 3: doCommit(args);
                break;
            case 4: dorm(args);
                break;
            case 5: dolog(args);
                break;
            case 6: dogloballog(args);
                break;
            case 7: dofind(args);
                break;
            case 8: dostatus(args);
                break;
            case 9: docheckout(args);
                break;
            case 10: doBranch(args);
                break;
            case 11: dormBranch(args);
                break;
            case 12: doreset(args);
                break;
            case 13: domerge1(args);
                break;
            default: exit(0);

            }
        }


    }
    /** Takes in ERROR for errors. */
    private static void generalerrors(String error) {
        if (error.equals("No Arguments")) {
            System.out.println("Please enter a command.");
            exit(0);
        }
        System.out.println("No command with that name exists.");
        exit(0);
    }

    /** Create a new Gitlet version control system in the
     * current directory. Uses ARGS. */
    private static void doinit(String... args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        boolean exists = directory.mkdir();
        if (!exists) {
            System.out.println("A gitlet version-control "
                    + "system already exists in the current directory.");
            exit(0);
        }
        Commit initialCommit = new Commit("initial commit", null, null);
        initialCommit.inittime();
        Gitlet structure = new Gitlet(true);
        structure.addCommit(initialCommit);
        structure.serializeitem(structure.getcommits(), commitlocale);
        Branch master = new Branch("master", initialCommit);
        master.activate();
        structure.addBranch(master);
        structure.serializeitem(structure.getBranches(), branchlocale);
    }

    /**Adds a copy of the file as it currently exists
     * to the staging area using ARGS. */
    private static void doadd(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        String addfile = args[1];
        if (!Utils.plainFilenamesIn(workdir).contains(addfile)) {
            System.out.println("File does not exist");
            exit(0);
        }
        File toadd = new File(addfile);
        Gitlet structure = new Gitlet(true);
        structure.getBranches(branchlocale);
        structure.getstage(stagelocale);
        structure.getuntracked(untrackedlocale);
        Commit active = structure.getactiveBranch().pointer();
        for (int i = 0; i < structure.untracked().size(); i++) {
            if (structure.untracked().get(i).getName().equals(
                    toadd.getName())) {
                structure.removeuntracked(structure.untracked().get(i));
                structure.serializeitem(structure.untracked(), untrackedlocale);
                exit(0);
            }
        }
        if (active.getblobs() != null) {
            String shacon = Utils.sha1(Utils.readContents(toadd));
            File tempshacon = new File(shacon);
            if (active.getblobs().hasexact(tempshacon)
                    && active.getblobs().hasfile(addfile)
                    && active.getblobs().getchild(toadd.toString())
                    .filesha1().equals(shacon)) {
                structure.removestaged(toadd);
                structure.serializeitem(structure.getstaged(), stagelocale);
                exit(0);
            }
        }
        structure.stagefile(toadd);
        structure.serializeitem(structure.getstaged(), stagelocale);
        structure.getuntracked(untrackedlocale);
        structure.removeuntracked(toadd);
        structure.serializeitem(structure.untracked(), untrackedlocale);
    }
    /**Saves a snapshot of certain files in the current
     * Commit and staging area so they can be restored
     * at a later time.  Uses ARGS. */
    private static void doCommit(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        if (args[1].isEmpty()) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }
        Gitlet structure = new Gitlet(true);
        structure.getstage(stagelocale);
        structure.getuntracked(untrackedlocale);
        if (structure.getstaged().isEmpty()
                && structure.untracked().isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }
        String message = args[1];
        structure.getcommits(commitlocale);
        structure.getBranches(branchlocale);
        Branch active = structure.getactiveBranch();
        Commit activeparent = active.pointer();
        Tree blobs = structure.stagedtoTree();
        Tree newblobs;
        if (blobs == null) {
            newblobs = activeparent.getblobs();
            newblobs = structure.untrackedtrimmed(newblobs);
        } else {
            if (activeparent.getblobs() != null) {
                structure.inherittostaged(blobs, activeparent.getblobs());
            }
            newblobs = structure.untrackedtrimmed(blobs);
        }
        Commit curr = new Commit(message, activeparent, newblobs);
        structure.addCommit(curr);
        structure.serializeitem(structure.getcommits(), commitlocale);
        structure.getBranches(branchlocale);
        active.repoint(curr);
        structure.addBranch(active);
        structure.serializeitem(structure.getBranches(), branchlocale);
        structure.clearstaged();
        structure.serializeitem(structure.getstaged(), stagelocale);
        structure.clearuntracked();
        structure.serializeitem(structure.untracked(), untrackedlocale);
    }

    /** Untrack a file, so that it is not to be
     * included in the next Commit. Uses ARGS. */
    private static void dorm(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        Gitlet structure = new Gitlet(true);
        structure.getBranches(branchlocale);
        structure.getstage(stagelocale);
        structure.getuntracked(untrackedlocale);
        ArrayList<Tree> staged = structure.getstaged();
        File rm = new File(args[1]);
        Commit last = structure.getactiveBranch().pointer();
        Tree lastblobs = last.getblobs();
        boolean commitcontains = false;
        for (int i = 0; i < staged.size(); i++) {
            if (staged.get(i).getname().equals(rm.getName())) {
                staged.get(i).getblob().delete();
                staged.remove(i);
                staged.trimToSize();
                structure.serializeitem(staged, stagelocale);
                exit(0);
            }
        }
        if (lastblobs != null && lastblobs.containsfile(rm)) {
            commitcontains = true;
            structure.adduntracked(rm);
            structure.serializeitem(structure.untracked(), untrackedlocale);
            Utils.restrictedDelete(rm);

        }
        if (!commitcontains) {
            System.out.println("No reason to remove the file.");
        }
    }

    /** Starting at the current head Commit, display
     * information about each Commit backwards along
     * the Commit Tree until the initial Commit using ARGS. */
    private static void dolog(String... args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        Gitlet structure = new Gitlet(true);
        structure.getBranches(branchlocale);
        Branch active = structure.getactiveBranch();
        Commit curr = active.pointer();
        while (curr != null) {
            System.out.println("===");
            System.out.println("commit " + curr.getsha());
            if (curr.getmm() != null) {
                System.out.println("Merge: " + curr.getmerges());
                System.out.println(curr.getdate());
                System.out.println(curr.getmessage());
            } else {
                System.out.println(curr.getdate());
                System.out.println(curr.getmessage());
            }
            System.out.println("");
            curr = curr.getparent();
        }

    }

    /** Like log, but displays the history of all commits ever
     * made using ARGS. */
    private static void dogloballog(String... args) {
        isworkingdirectory();
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        Gitlet structure = new Gitlet(true);
        structure.getcommits(commitlocale);
        ArrayList<Commit> allcomits = structure.getcommits();
        for (int i = 0; i < allcomits.size(); i++) {
            Commit curr = allcomits.get(i);
            System.out.println("===");
            System.out.println("commit " + curr.getsha());
            if (curr.getmm() != null) {
                System.out.println("Merge: " + curr.getmerges());
                System.out.println(curr.getdate());
                System.out.println(curr.getmessage());
            } else {
                System.out.println(curr.getdate());
                System.out.println(curr.getmessage());
            }
            System.out.println("");
        }
    }

    /**Prints out the ids of all commits that have the
     * given Commit message, one per line.
     * Uses ARGS.*/
    private static void dofind(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        String message = args[1];
        boolean found = false;
        Gitlet structure = new Gitlet(true);
        structure.getcommits(commitlocale);
        ArrayList<Commit> allcomits = structure.getcommits();
        for (int i = 0; i < allcomits.size(); i++) {
            Commit curr = allcomits.get(i);
            if (curr.getmessage().equals(message)) {
                System.out.println(curr.getsha());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what Branches currently exist, and
     * marks the current Branch with a *.  Also displays
     * what files have been staged or marked for untracking.
     * Uses ARGS.*/
    private static void dostatus(String... args) {
        isworkingdirectory();
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        Gitlet structure = new Gitlet(true);
        structure.getBranches(branchlocale);
        structure.printBranches();
        structure.getstage(stagelocale);
        structure.printstaged();
        structure.getuntracked(untrackedlocale);
        structure.printuntracked();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");
        System.out.println("");
    }

    /** There are three arguments for checkout.
     * [file name] takes the version of the file as it exists in the head Commit
     * and puts it in the working directory, overwriting the version of the
     * file that's already there if there is one.
     * [Commit id] -- [file name] takes the version as it exists
     * in the Commit with the given id, and puts it in the...
     * [Branch name] takes all files in the Commit at the head of the
     * given Branch, and puts them in the working directory, overwriting
     * the versions of the files that are already there if they exist.
     * Uses ARGS. */
    private static void docheckout(String... args) {
        isworkingdirectory();
        Gitlet structure = new Gitlet(true);
        structure.getBranches(branchlocale);
        if (args.length == 3
                && args[1].equals("--")) {
            checkout1(args[2], structure);
        } else if (args.length == 4
                && args[2].equals("--")) {
            checkout2(args[1], args[3]);
        } else if (args.length == 2) {
            checkout3(args[1]);
        } else {
            System.out.println("Incorrect operands.");
            exit(0);
        }
    }
    /** First checkout. Uses NAME and STRUCTURE. */
    private static void checkout1(String name, Gitlet structure) {
        Branch active = structure.getactiveBranch();
        Commit curr = active.pointer();
        Tree files = curr.getblobs();
        Tree blobs = files;
        boolean found = false;
        while (blobs != null) {
            if (blobs.getname().equals(name)) {
                File from = blobs.getblob();
                File to = new File(name);
                try {
                    Files.copy(from.toPath(), to.toPath(), REPLACE_EXISTING);
                    found = true;
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
            blobs = blobs.getchild();
        }
        if (!found) {
            System.out.println("File does not exist");
            exit(0);
        }
    }
    /** Second checkout. Uses ID and NAME. */
    private static void checkout2(String id, String name) {
        Gitlet structure = new Gitlet(true);
        structure.getcommits(commitlocale);
        ArrayList<Commit> past = structure.getcommits();
        Commit found = null;
        for (int i = 1; i < past.size(); i++) {
            String comp1 = past.get(i).getsha();
            comp1 = comp1.substring(0, id.length());
            if (comp1.equals(id)) {
                found = past.get(i);
            }
        }
        if (found == null) {
            System.out.println("No commit with that id exists.");
            exit(0);
        }
        Tree files = found.getblobs();
        Tree blobs = files;
        boolean foundit = false;
        while (blobs != null) {
            if (blobs.getname().equals(name)) {
                File from = blobs.getblob();
                File to = new File(name);
                try {
                    Files.copy(from.toPath(), to.toPath(), REPLACE_EXISTING);
                    foundit = true;
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
            blobs = blobs.getchild();
        }
        if (!foundit) {
            System.out.println("File does not exist in that Commit.");
            exit(0);
        }
        structure.getuntracked(untrackedlocale);
        for (int i = 0; i < structure.untracked().size(); i++) {
            if (structure.untracked().get(i).toString().equals(name)) {
                structure.untracked().remove(i);
                structure.untracked().trimToSize();
                structure.serializeitem(structure.untracked(), untrackedlocale);
            }
        }
    }
    /** Third checkout. Uses BRANCH. */
    private static void checkout3(String branch) {
        Gitlet structure = new Gitlet(true); Branch given = null;
        structure.getBranches(branchlocale);
        structure.getuntracked(untrackedlocale);
        for (int i = 0; i < structure.untracked().size(); i++) {
            if (structure.untracked().get(i).isFile()) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it or add it first.");
                exit(0);
            }
        }
        ArrayList<Branch> branches = structure.getBranches();
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).getname().equals(branch)) {
                given = branches.get(i); break;
            }
        }
        if (given == null) {
            System.out.println("No such branch exists."); exit(0);
        }
        if (given.equals(structure.getactiveBranch())) {
            System.out.println("No need to checkout the current branch");
            exit(0);
        }
        Branch old = structure.getactiveBranch();
        Tree oldcurr = old.pointer().getblobs();
        Tree newgiven = given.pointer().getblobs();
        while (newgiven != null) {
            if (new File(newgiven.getname()).isFile()) {
                if (oldcurr == null || !oldcurr.hasfile(newgiven.getname())) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it or add it first.");
                    exit(0);
                }
            }
            newgiven = newgiven.getchild();
        }
        newgiven = given.pointer().getblobs();
        if (newgiven == null) {
            while (oldcurr != null) {
                Utils.restrictedDelete(oldcurr.getname());
                oldcurr = oldcurr.getchild();
            }
        }
        while (oldcurr != null) {
            if (!newgiven.hasfile(oldcurr.getname())) {
                Utils.restrictedDelete(oldcurr.getname());
            }
            oldcurr = oldcurr.getchild();
        }
        structure.getactiveBranch().deactivate(); given.activate();
        Tree now = given.pointer().getblobs();
        while (now != null) {
            checkout1(now.getname(), structure); now = now.getchild();
        }
        structure.getstage(stagelocale); structure.clearstaged();
        structure.serializeitem(structure.getBranches(), branchlocale);
        structure.serializeitem(structure.getstaged(), stagelocale);
    }
    /** Creates a new Branch with the given name, and points it at the current
     * head node.  A Branch is nothing more than a name for a reference to a
     * Commit node. Uses ARGS. */
    private static void doBranch(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        }
        Gitlet structure = new Gitlet(true);
        String branchname = args[1];
        structure.getBranches(branchlocale);
        for (int i = 0; i < structure.getBranches().size(); i++) {
            if (structure.getBranches().get(i).getname().equals(branchname)) {
                System.out.println("A branch with that name already exists.");
                exit(0);
            }
        }
        Commit curr = structure.getactiveBranch().pointer();
        Branch newBranch = new Branch(branchname, curr);
        structure.addBranch(newBranch);
        structure.serializeitem(structure.getBranches(), branchlocale);

    }
    /**Deletes the Branch with the given name.  This only means to delete
     * the pointer associated with the Branch. Uses ARGS*/
    private static void dormBranch(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        }
        Gitlet structure = new Gitlet(true);
        String branchname = args[1];
        structure.getBranches(branchlocale);
        if (structure.getactiveBranch().getname().equals(branchname)) {
            System.out.println("Cannot remove the current branch");
            exit(0);
        }
        if (!structure.deleteBranch(branchname)) {
            System.out.println("A branch with that name does not exist");
            exit(0);
        }
        structure.serializeitem(structure.getBranches(), branchlocale);
    }
    /**Checks out all the files tracked by the given Commit.  Removes
     * tracked files that are not present in that Commit. Uses ARGS. */
    private static void doreset(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        String id = args[1];
        Gitlet structure = new Gitlet(true);
        structure.getuntracked(untrackedlocale);
        for (int i = 0; i < structure.untracked().size(); i++) {
            if (structure.untracked().get(i).isFile()) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it or add it first.");
                exit(0);
            }
        }
        structure.getBranches(branchlocale);
        structure.getcommits(commitlocale);
        Commit foundcom = null;
        for (int i = 0; i < structure.getcommits().size(); i++) {
            if (structure.getcommits().get(i).getsha().equals(id)) {
                foundcom = structure.getcommits().get(i);
            }
        }
        if (foundcom == null) {
            System.out.println("No commit with that id exists.");
            exit(0);
        }
        Tree curr = foundcom.getblobs();
        Tree given = structure.getactiveBranch().pointer().getblobs();
        ifTreeuntracked(given, curr);
        while (given != null) {
            if (!curr.hasfile(given.getname())) {
                Utils.restrictedDelete(given.getname());
            }
            given = given.getchild();
        }
        while (curr != null) {
            checkout2(foundcom.getsha(), curr.getname());
            curr = curr.getchild();
        }
        structure.getstage(stagelocale);
        structure.clearstaged();
        structure.serializeitem(structure.getstaged(), stagelocale);
        structure.getactiveBranch().repoint(foundcom);
        structure.serializeitem(structure.getBranches(), branchlocale);
    }

    /**Merges files from the given Branch into the current Branch.
     * Uses String... ARGS.  */
    private static void domerge1(String... args) {
        isworkingdirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        }
        String branchname = args[1];
        Gitlet structure = new Gitlet(true);
        structure.getBranches(branchlocale);
        Branch current = structure.getactiveBranch();
        if (current.getname().equals(branchname)) {
            System.out.println("Cannot merge a branch with itself.");
            exit(0);
        }
        Branch given = structure.getBranch(branchname);
        structure.getuntracked(untrackedlocale);
        structure.getstage(stagelocale);
        if (!structure.getstaged().isEmpty()
                || !structure.untracked().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            exit(0);
        }
        for (int i = 0; i < structure.untracked().size(); i++) {
            if (structure.untracked().get(i).isFile()) {
                System.out.println("There is an untracked file"
                        + " in the way; delete it or add it first.");
                exit(0);
            }
        }
        ifuntracked(current, given);
        String mm = current.pointer().getsha() + " " + given.pointer().getsha();
        Commit split = given.findsplit(current);
        if (split.getsha().equals(given.pointer().getsha())) {
            System.out.println("Given branch is an ancestor of"
                    + " the current branch.");
            exit(0);
        } else if (split.getsha().equals(current.pointer().getsha())) {
            current.repoint(given.pointer());
            structure.serializeitem(structure.getBranches(), branchlocale);
            System.out.println("Current branch fast-forwarded.");
            exit(0);
        }
        merge2(current, given, split, structure, branchname, mm);
    }
    /** Second part of merge. *
     * Takes in CURRENT GIVEN SPLIT STRUCTURE MM and BRANCHNAME. */
    private static void merge2(Branch current, Branch given,
                               Commit split, Gitlet structure,
                               String branchname,
                               String mm) {
        Tree old = current.pointer().getblobs();
        Tree news = given.pointer().getblobs();
        Tree splitTree = split.getblobs();
        ArrayList<String> oldmodified = old.modified(splitTree);
        ArrayList<String> newmodified = news.modified(splitTree);
        boolean conflict = false;
        while (old != null) {
            if (!news.hasfile(old.getname()) && splitTree.hasfile(old.getname())
                    && !splitTree.hasexact(old.getblob())) {
                conflict = true;
                mergeconflict(old.getname(), old, news, structure);
            }
            old = old.getchild();
        }
        old = current.pointer().getblobs();
        for (int i = 0; i < newmodified.size(); i++) {
            String moddedname = newmodified.get(i);
            if (!oldmodified.contains(moddedname)) {
                newmodified.remove(i);
                checkout2(given.pointer().getsha(), moddedname);
                structure.stagefile(new File(moddedname));
                structure.serializeitem(structure.getstaged(), stagelocale);
            }
        }
        newmodified.trimToSize();
        for (int i = 0; i < oldmodified.size(); i++) {
            String moddedname = oldmodified.get(i);
            if (!newmodified.contains(moddedname)) {
                oldmodified.remove(i);
            }
        }
        ArrayList<String> notpresent = news.splitmissing(splitTree);
        for (int i = 0; i < notpresent.size(); i++) {
            checkout2(given.pointer().getsha(), notpresent.get(i));
            structure.stagefile(new File(notpresent.get(i)));
            structure.serializeitem(structure.getstaged(), stagelocale);
        }
        structure.getuntracked(untrackedlocale);
        ArrayList<String> absent = splitTree.splitmissing(news);
        for (int i = 0; i < absent.size(); i++) {
            if (splitTree.unmodified(old, absent.get(i))) {
                Utils.restrictedDelete(absent.get(i));
                structure.adduntracked(new File(absent.get(i)));
            }
        }
        for (int i = 0; i < oldmodified.size(); i++) {
            if (old.hasfile(oldmodified.get(i))
                    && news.hasfile(oldmodified.get(i))
                    && !old.hasexact
                    (news.getchild(oldmodified.get(i)).getblob())) {
                conflict = true;
                mergeconflict(oldmodified.get(i), old, news, structure);
            }
        }
        merge3(branchname, conflict, current, given, structure, mm);
    }
    /** Last of merge.  Use BRANCHNAME CONFLICT
     * CURRENT GIVEN STRUCTURE and MM. */
    private static void merge3(String branchname, boolean conflict,
                               Branch current, Branch given,
                               Gitlet structure,
                               String mm) {
        String mergemessage = "Merged " + branchname + " into "
                + current.getname() + ".";
        String merge1 = current.pointer().getsha();
        String merge2 = given.pointer().getsha();
        doCommit("Null", mergemessage);
        if (conflict) {
            System.out.println("Encountered a merge conflict");
        }
        structure.getBranches(branchlocale);
        current = structure.getactiveBranch();
        current.pointer().setmergemessage(mm);
        current.pointer().settwomerges(merge1, merge2);
        structure.serializeitem(structure.getBranches(), branchlocale);
    }
    /** Take care of merges. Uses a String NAME, Tree CURRENT,
     * Tree GIVEN, and Gitlet STRUCTURE. */
    private static void mergeconflict(String name,
                                      Tree current,
                                      Tree given,
                                      Gitlet structure) {
        File temp = new File(name);
        try {
            PrintWriter writer = new PrintWriter(temp);
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        try (Writer writer = new
                BufferedWriter(new
                OutputStreamWriter(new
                FileOutputStream(temp),
                StandardCharsets.UTF_8))) {
            writer.write("<<<<<<< HEAD\r\n");
            if (current.hasfile(name)) {
                File write = current.getchild(name).getblob();
                String towrite = Utils.readContentsAsString(write);
                writer.write(towrite);
            }
            writer.write("=======\r\n");
            if (given.hasfile(name)) {
                File write = given.getchild(name).getblob();
                String towrite = Utils.readContentsAsString(write);
                writer.write(towrite);
            }
            writer.write(">>>>>>>\r\n");
            structure.stagefile(temp);
            structure.serializeitem(structure.getstaged(), stagelocale);
        }
        catch (IOException ex) {
            System.out.println(ex);
        }


    }
    /** Error if this is not a working directory. */
    private static void isworkingdirectory() {
        if (!directory.isDirectory()) {
            System.out.println("Not in an initialized gitlet directory.");
            exit(0);
        }
    }
    /**If there is an untracked file.
     * using Branch OLD and Branch GIVEN.*/
    private static void ifuntracked(Branch old, Branch given) {
        Tree oldcurr = old.pointer().getblobs();
        Tree newgiven = given.pointer().getblobs();
        while (newgiven != null) {
            if (new File(newgiven.getname()).isFile()) {
                if (oldcurr == null
                        || !oldcurr.hasfile(newgiven.getname())) {
                    System.out.println("T"
                            + "here is an untracked f"
                            + "ile in the way; de"
                            + "lete it or add it first.");
                    exit(0);
                }

            }
            newgiven = newgiven.getchild();
        }
    }
    /**If there is an untracked file, using Trees.
     * Using Tree OLD and Tree GIVEN. */
    private static void ifTreeuntracked(Tree old, Tree given) {
        while (given != null) {
            if (new File(given.getname()).isFile()) {
                if (old == null
                        || !old.hasfile(given.getname())) {
                    System.out.println("There is"
                            + " an untracked file in the way; delete "
                            + "it or add it first.");
                    exit(0);
                }

            }
            given = given.getchild();
        }
    }
    /** List of Gitlet arguments. */
    private static HashMap<String, Integer> _arguments = new HashMap<>();
    static {
        _arguments.put("init", 1);
        _arguments.put("add", 2);
        _arguments.put("commit", 3);
        _arguments.put("rm", 4);
        _arguments.put("log", 5);
        _arguments.put("global-log", 6);
        _arguments.put("find", 7);
        _arguments.put("status", 8);
        _arguments.put("checkout", 9);
        _arguments.put("branch", 10);
        _arguments.put("rm-branch", 11);
        _arguments.put("reset", 12);
        _arguments.put("merge", 13);
    }
    /** File directory. */
    private static File directory = new File(".gitlet");
    /** String Commitlcale. */
    private static String commitlocale = directory.toPath() + "\\commits";
    /** String branchlocale. */
    private static String branchlocale = directory.toPath() + "\\Branches";
    /** String stagelocale. */
    private static String stagelocale = directory.toPath() + "\\staging area";
    /** String untrackedlocale. */
    private static String untrackedlocale = directory.toPath()
            + "\\untrackedfiles";
}
