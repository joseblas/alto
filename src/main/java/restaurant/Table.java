package restaurant;

public class Table {
    public final int size; // number of chairs
    public int occupiedSeats;

    public Table(int size) {
        this.size = size;
        this.occupiedSeats = 0;
    }

    public Table(int size, int occupiedSeats) {
        this.size = size;
        this.occupiedSeats = occupiedSeats;
    }

    public boolean canAccommodate(ClientsGroup party) {
        return size - occupiedSeats >= party.size;
    }

    public void occupy(ClientsGroup group) {
        occupiedSeats += group.size;
    }

    public void release(ClientsGroup group) {
        occupiedSeats -= group.size;
    }

    @Override
    public String toString() {
        return "Table{" + "size=" + size + ", occupiedSeats=" + occupiedSeats + '}';
    }

    public int available() {
        return size - occupiedSeats;
    }

    public boolean isEmpty() {
        return occupiedSeats == 0;
    }

    public int getSize() {
        return size;
    }

}
