package restaurant;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Note: interface is not created because is not needed yet.
public class RestManager {
    final List<Table> tables;
    Queue<ClientsGroup> queue;
    final Map<ClientsGroup, Table> seatingMap;
    final Lock lock = new ReentrantLock();

    // Note: In the instructions, the list of tables is hardcoded as a list of 5 tables.
    // However, in the code, the list of tables is passed as a parameter as instructed in the code snippet.
    public RestManager(List<Table> tables) {
        this.tables = new ArrayList<>(tables);
        this.queue = new LinkedList<>();
        this.seatingMap = new HashMap<>();
        this.tables.sort(Comparator.comparingInt(t -> t.size)); // Sort tables by size
    }

    // New party shows up
    public void onArrive(ClientsGroup group) {
        lock.lock();
        try {
            System.out.println("Group arrived: " + group);
            if (assignTable(group)) {
                System.out.println("Group seated immediately: " + group);
            } else {
                System.out.println("Group added to queue: " + group);
                queue.add(group);
            }
        } finally {
            lock.unlock();
        }
    }

    // Party leaves
    public void onLeave(ClientsGroup group) {
        Table table;
        lock.lock();
        try {
            System.out.println("Group leaving: " + group);
            table = seatingMap.remove(group);
            if (table != null) {
                table.release(group);
                System.out.println("Group left and table released: " + table + " " + group);
            } else {
                if (!queue.remove(group)) {
                    throw new IllegalStateException("Group not in queue: " + group);
                }
                System.out.println("Group left the queue: " + group);
            }
        } finally {
            lock.unlock();
        }

        if (table != null) {
            processQueue(table);
        }
    }

    // Return table where a given client group is seated
    public Table lookup(ClientsGroup group) {
        lock.lock();
        Table table;
        try {
            table = seatingMap.get(group);
        } finally {
            lock.unlock();
        }
        return table;
    }

    // Process the queue and seat waiting groups
    private void processQueue(Table table) {
        if (queue.isEmpty()) {
            //avoid to lock if queue is empty
            return;
        }

        lock.lock();
        try {
            int availableSeats = table.available();
            List<ClientsGroup> toRemove = new ArrayList<>() ;
            for (ClientsGroup clientsGroup : queue) {
                if (clientsGroup.size <= availableSeats) {
                    if (assignTable(clientsGroup)) {
                        System.out.println("Seated from queue: " + clientsGroup);
                        toRemove.add(clientsGroup);
                        availableSeats = table.available();
                    }
                }
            }
            queue.removeAll(toRemove);
        } finally {
            lock.unlock();
        }
    }

    // Assign a table to a group, either from the queue or a new group
    boolean assignTable(ClientsGroup group) {
        // sort by isEmpty and size
        Table assingedTable = null;
        for (Table table : tables) {
            if (table.available() >= group.size) {
                if (table.canAccommodate(group)) {
                    // empty tables are preferred.
                    // Note: assign the smallest table that can accommodate the group
                    // not in the requirements, but it makes sense
                    if (table.isEmpty()) {
                        assingedTable = table;
                        break;
                    } else {
                        // assign the smallest table that can accommodate the group
                        // not in the requirements, but it makes sense
                        assingedTable = table;
                    }
                }
            }
        }

        if (assingedTable != null) {
            assingedTable.occupy(group);
            seatingMap.put(group, assingedTable);
            return true;
        }

        return false;
    }
}

