
package restaurant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class RestManagerTest {

    List<Table> tables = List.of(new Table(2), new Table(4), new Table(6));

    RestManager manager;

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        System.out.println("**********************************************");
        System.out.println("Running test: " + testInfo.getDisplayName());
        System.out.println("Tables: " + tables);
    }

    @Test
    void partyLargerThan6() {
        manager = new RestManager(tables);
        assertThrows(IllegalArgumentException.class, () -> new ClientsGroup(20));
    }

    @Test
    public void fourChairProblem(){
        manager = new RestManager(List.of(new Table(4), new Table(6)));
        ClientsGroup group1 = new ClientsGroup(2);

        manager.onArrive(group1);
        hasTable(group1);

        ClientsGroup group2 = new ClientsGroup(2);
        manager.onArrive(group2);

        Table table = manager.seatingMap.get(group2);
        assertThat(table.size).isEqualTo(6);
    }


    @Test
    void partyInQueueIsAssignedInArrivalOrder() {
        manager = new RestManager(tables);
        ClientsGroup group1 = new ClientsGroup(6);
        ClientsGroup group2 = new ClientsGroup(6);
        ClientsGroup group3 = new ClientsGroup(4);
        ClientsGroup group4 = new ClientsGroup(4);
        ClientsGroup group5 = new ClientsGroup(4);
        ClientsGroup group6 = new ClientsGroup(6);


        manager.onArrive(group1); //gets seated
        hasTable(group1);

        manager.onArrive(group2);// waits in queue
        manager.onArrive(group3); // gets seated
        manager.onArrive(group4); // gets in queue
        manager.onArrive(group5); // gets in queue
        manager.onArrive(group6); // gets in queue

        isWaiting(group2);

        hasTable(group3);
        isWaiting(group4);

        assertQueueSize(4);

        manager.onLeave(group3);

        assertQueueSize(3);
        hasTable(group4);

        manager.onLeave(group1);

        assertQueueSize(2);
        hasTable(group2);

        areWaiting(group5, group6);
    }

    @Test
    void partyNotAssigned() {
        manager = new RestManager(tables);
        ClientsGroup group = new ClientsGroup(6);
        ClientsGroup groupToWait = new ClientsGroup(6);

        manager.onArrive(group);
        manager.onArrive(groupToWait);

        isWaiting(groupToWait);
        assertQueueSize(1);

        manager.onLeave(group);
        assertQueueSize(0);

        alreadyLeft(group);
    }

    @Test
    void testGroupArrivalAndSeating() {
        manager = new RestManager(tables);
        ClientsGroup group2 = new ClientsGroup(2);
        ClientsGroup group4 = new ClientsGroup(4);
        ClientsGroup group6 = new ClientsGroup(6);

        manager.onArrive(group6);
        manager.onArrive(group4);
        manager.onArrive(group2);

        hasTable(group2, group4, group6);

    }

    @Test
    public void accomodateTwoPartiesAfterOneLeft(){
        manager = new RestManager(tables);// 2 4 6
        ClientsGroup group2 = new ClientsGroup(2);
        ClientsGroup group4 = new ClientsGroup(4);
        ClientsGroup group6 = new ClientsGroup(6);

        manager.onArrive(group6);
        manager.onArrive(group4);
        manager.onArrive(group2);

        hasTable(group2, group4, group6);

        ClientsGroup group2_1_new = new ClientsGroup(2);
        ClientsGroup group2_2_new = new ClientsGroup(2);
        ClientsGroup group2_3_new = new ClientsGroup(3);
        ClientsGroup group2_4_new = new ClientsGroup(2);

        // three parties of 2 are waiting
        manager.onArrive(group2_1_new);
        manager.onArrive(group2_2_new);
        manager.onArrive(group2_3_new);
        manager.onArrive(group2_4_new);

        manager.onLeave(group6);
        alreadyLeft(group6);
        hasTable(group2, group4, group2_1_new, group2_2_new, group2_4_new);

        isWaiting(group2_3_new);
        assertQueueSize(1);
    }

    @Test
    public void partyGoToEmptyTableFirst() {
        List<Table> tables = List.of(new Table(4, 1), new Table(2), new Table( 6));
        manager = new RestManager(tables);
        ClientsGroup group = new ClientsGroup(2);

        manager.onArrive(group);

        hasTable(group);

        assertThat(manager.lookup(group).size).isEqualTo(2);

    }

    @Test
    void testOnArriveMorePartiesAndLookup() {
        manager = new RestManager(tables);
        ClientsGroup group2 = new ClientsGroup(2);
        ClientsGroup group5 = new ClientsGroup(5);
        ClientsGroup group6 = new ClientsGroup(6);

        manager.onArrive(group2);
        manager.onArrive(group5);
        manager.onArrive(group6);

        // Assuming that a party of 2 can be seated at a table of size 2, better than a greater table
        hasTable(group2, group5);

        assertQueueSize(1);

        //party of 5 leave making room for a party of 6
        manager.onLeave(group5);

        //is gone party of 5
        alreadyLeft(group5);
        hasTable(group6);

        assertQueueSize(0);
    }

    @Test
    void testSharingTables() {
        manager = new RestManager(tables); // 2 4 6
        ClientsGroup group2_1 = new ClientsGroup(2); // table 2
        ClientsGroup group2_2 = new ClientsGroup(2); // table 4
        ClientsGroup group2_3 = new ClientsGroup(2); // table 6
        ClientsGroup group6 = new ClientsGroup(6); // waits

        manager.onArrive(group2_1);
        manager.onArrive(group2_2);
        manager.onArrive(group2_3);
        manager.onArrive(group6);

        // Assuming that a party of 2 can be seated at a table of size 2, better than a greater table
        hasTable(group2_1, group2_2, group2_3);

        isWaiting(group6);
    }

    @Test
    void testOnLeave() {
        manager = new RestManager(List.of(new Table(2)));
        ClientsGroup group = new ClientsGroup(2);
        ClientsGroup waitingGroup = new ClientsGroup(6);

        manager.onArrive(group);
        manager.onArrive(waitingGroup);

        // Assuming that a party of 2 can be seated at a table of size 2, better than a greater table
        hasTable(group);

        manager.onLeave(group);
        alreadyLeft(group);

        manager.onLeave(waitingGroup);
        alreadyLeft(waitingGroup);
    }

    @Test
    public void leavePartyOf2ThatNeverArrived() {
        manager = new RestManager(tables);
        ClientsGroup group = new ClientsGroup(2);
        assertThrows(IllegalStateException.class, () -> manager.onLeave(group));
    }

    @Test
    void testConcurrencyAndRaceConditions() throws InterruptedException {

        List<Table> tables = Arrays.asList(
                new Table(2),
                new Table(3),
                new Table(4),
                new Table(5),
                new Table(6)
        );
        manager = new RestManager(tables);

        // Initialize various client groups
        ClientsGroup group1 = new ClientsGroup(2);
        ClientsGroup group2 = new ClientsGroup(4);
        ClientsGroup group3 = new ClientsGroup(6);
        ClientsGroup group4 = new ClientsGroup(3);
        ClientsGroup group5 = new ClientsGroup(2);
        ClientsGroup group6 = new ClientsGroup(5);

        // Use an ExecutorService to test concurrent access
        ExecutorService executor = Executors.newFixedThreadPool(10);

        executor.submit(() -> manager.onArrive(group1));
        executor.submit(() -> manager.onArrive(group2));
        executor.submit(() -> manager.onArrive(group3));
        executor.submit(() -> manager.onArrive(group4));

        assertQueueSize(0);

        hasTable(group2, group3);
        executor.submit(() -> manager.onArrive(group5));
        executor.submit(() -> manager.onArrive(group6));

        executor.submit(() -> manager.onLeave(group2));
        alreadyLeft(group2);

        executor.submit(() -> manager.onLeave(group3));
        alreadyLeft(group3);


        // Validate the state of the system after concurrent operations
        hasTable(group1, group4, group5, group6);

        assertQueueSize(0);

        // Wait for all tasks to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(500, TimeUnit.MILLISECONDS), "Executor tasks did not complete in time.");
        executor.close();
    }

    @Test
    void testEdgeCaseEmptyQueueHandling() {
        // Check system behavior with no arrivals
        manager = new RestManager(tables);
        assertNull(manager.lookup(new ClientsGroup(1)), "Group 1 should not be seated as it has not arrived.");
    }


    // Note: await has been used for the usage of threads.
    // There is no mention to thread but this code could be used off the self with threads
    // so I have used it to make sure that the code is working as expected.

    private void hasTable(ClientsGroup...groups) {
        for(ClientsGroup group: groups) {
            await().untilAsserted(() -> assertThat(manager.lookup(group)).isNotNull());
        }
    }

    private void areWaiting(ClientsGroup...groups) {
        for(ClientsGroup g: groups) {
            await().untilAsserted(() -> assertThat(manager.queue).contains(g));
        }
    }
    private void isWaiting(ClientsGroup group) {
        areWaiting(group);
    }

    private void assertQueueSize(int expectedSize) {
        await().untilAsserted(() -> assertThat(manager.queue.size()).isEqualTo(expectedSize));
    }

    private void alreadyLeft(ClientsGroup group) {
        await().untilAsserted(() -> assertThat(manager.lookup(group)).isNull());
    }

}
