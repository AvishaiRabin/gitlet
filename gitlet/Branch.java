package gitlet;
import java.io.Serializable;
import java.util.ArrayList;

/** Branch class. */
/** @author Avi Rabin. */
class Branch implements Serializable {
    /** The name of this branch. */
    private String _name;
    /** What Commit this branch is currently pointing at. */
    private Commit _pointer;
    /** Boolean whether this branch is active. */
    private boolean _active;
    /** Creating a branch. Uses NAME and POINTER. */
    Branch(String name, Commit pointer) {
        _name = name;
        _pointer = pointer;
        _active = false;
    }
    /** Return a boolean whether this branch is active. */
    public boolean isactive() {
        return _active;
    }
    /**Changing what this branch is pointing at.
     * Takes in the argument NEWPOINTER. */
    public void repoint(Commit newpointer) {
        _pointer = newpointer;
    }
    /** Return the Commit this branch is pointing at. */
    public Commit pointer() {
        return _pointer;
    }
    /** Deactivate this branch. */
    public void deactivate() {
        _active = false;
    }
    /** Activate this branch. */
    public void activate() {
        _active = true;
    }
    /** Return this branch's name. */
    public String getname() {
        return _name;
    }
    /**Find the splitting point between this
     * branch and another one. Given a branch OTHER.
     * Return this Commit. */
    public Commit findsplit(Branch other) {
        ArrayList<Commit> ours = new ArrayList<>();
        Commit ourcom = pointer();
        while (!(ourcom == null)) {
            ours.add(ourcom);
            ourcom = ourcom.getparent();
        }
        Commit potential = other.pointer();
        while (true) {
            for (int i = 0; i < ours.size(); i++) {
                if (ours.get(i).getsha().equals(potential.getsha())) {
                    return potential;
                }
            }
            potential = potential.getparent();
        }
    }
}
