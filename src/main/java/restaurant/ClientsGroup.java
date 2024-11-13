
package restaurant;

public class ClientsGroup {
    public final int size;

    public ClientsGroup(int size) {
        // Note: this limit should be a configurable value
        if(size <= 0 || size > 6) {
            throw new IllegalArgumentException("Illegal Group size %s".formatted(size));
        }
        this.size = size;
    }

    @Override
    public String toString() {
        return "ClientsGroup{" +
                "size=" + size +
                '}';
    }
}
