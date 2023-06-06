import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;


class FIFO {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";
    int pageFault;
    LinkedList<Integer> seats;
    int capacity;
    int pointer;


    public FIFO(int n) {
        this.pageFault = 0;
        this.seats = new LinkedList<>();
        this.capacity = n;
        this.pointer = 0;
    }

    public void add(int customer) {
        if (seats.contains(customer)) return;

        pageFault++;
        if (seats.size() == capacity) {
            seats.remove(pointer);
            seats.add(pointer++, customer);
            if (pointer == seats.size()) pointer = 0;
        } else seats.add(customer);
    }

    public void print(boolean debug) {
        System.out.print(ANSI_GREEN + "\tFIFO      \t" + ANSI_BLUE);
        //System.out.print(ANSI_BLUE + "Seats: ");
        for (int seat : seats) System.out.print(seat + " ");
        System.out.print(ANSI_RED + "\t\t" + this.pageFault);
        if (debug)
            System.out.print(ANSI_CYAN + "\t" + seats.get(pointer));
        System.out.println(ANSI_RESET + "\n");
    }
}

class LRU extends FIFO {
    LinkedList<Integer> stack;

    public LRU(int n) {
        super(n);
        this.stack = new LinkedList<>();
    }

    protected int findLRU() {
        int lru = stack.poll();
        return seats.indexOf(lru);

    }

    protected boolean alreadyIn(int customer) {
        for (int i = 0; i < stack.size(); i++) {
            if (stack.get(i) == customer) {
                stack.remove(i);
                stack.add(customer);
                return true;
            }
        }
        return false;
    }

    @Override
    public void add(int customer) {
        if (alreadyIn(customer)) return;

        pageFault++;
        if (capacity == seats.size()) {
            int idx = findLRU();
            seats.remove(idx);
            seats.add(idx, customer);
        } else {
            seats.add(customer);
        }


        stack.add(customer);
    }

    @Override
    public void print(boolean debug) {
        System.out.print(ANSI_GREEN + "\tLRU       \t" + ANSI_BLUE);
        //System.out.print(ANSI_BLUE + "\tSeats: ");
        for (int seat : seats) System.out.print(seat + " ");
        System.out.print(ANSI_RED + "\t\t" + this.pageFault);
        if (debug) {
            System.out.print(ANSI_PURPLE + "\n\tStack     \t");
            for (int st : stack) System.out.print(st + " ");
        }
        System.out.println(ANSI_RESET + "\n");
    }

}

class SecondChance extends FIFO {
    protected static class Pair {
        int val;
        boolean chance;

        public Pair(int val, boolean chance) {
            this.val = val;
            this.chance = chance;
        }
    }

    LinkedList<Pair> seat;

    public SecondChance(int n) {
        super(n);
        this.seat = new LinkedList<>();
        this.pointer = 0;
    }

    protected int findVictim() {
        for (int i = pointer; i < seat.size(); i++) {
            if (!seat.get(i).chance) {
                pointer = (i + 1) % capacity;
                return i;
            }
            seat.get(i).chance = false;
        }

        for (int i = 0; i < pointer; i++) {
            if (!seat.get(i).chance) {
                pointer = (i + 1) % capacity;
                return i;
            }
            seat.get(i).chance = false;
        }


        return pointer++ % capacity;
    }

    @Override
    public void add(int customer) {
        for (Pair i : seat)
            if (i.val == customer) {
                i.chance = true;
                return;
            }



        pageFault++;
        if (capacity == seat.size()) {
            int idx = findVictim();
            //Replace:
            seat.get(idx).chance = false;
            seat.get(idx).val = customer;
        }

        else seat.add(new Pair(customer, false));

    }

    @Override
    public void print(boolean debug) {
        System.out.print(ANSI_GREEN + "\t2nd Chance\t" + ANSI_BLUE);
        //System.out.print(ANSI_BLUE + "\tSeats:  ");
        for (Pair seat : seat) System.out.print(seat.val + " ");
        System.out.print(ANSI_RED + "\t\t" + this.pageFault);
        if (debug) {
            System.out.print(ANSI_CYAN + "\t" + pointer);
            System.out.print(ANSI_PURPLE + "\n\tChances   \t");
            for (Pair seat : seat) System.out.print(seat.chance ? "Y  " : "N  ");

        }
        System.out.println(ANSI_RESET + "\n");
    }
}

public class Client {
    static FIFO f, l, se;
    static final PC pc = new PC();
    static Thread t1, t2;

    public static void main(String[] args) throws InterruptedException {
        //System.out.println(Long.MAX_VALUE);
        //System.out.println(System.currentTimeMillis());
        //Testing Algorithms:

        //int[] reference = {13, 12, 10, 11, 12, 10, 12, 6, 12, 15, 8, 9, 12, 12, 6, 5, 14, 11, 8, 7, 11, 10, 10, 13, 15, 11, 6, 11, 8, 12, 14};
        /*int[] reference = {12, 11, 6, 6, 15, 12, 10, 8, 13, 9, 10, 15, 14, 9, 5, 14, 14, 13, 6, 8, 7, 9, 5, 6, 13, 15, 7, 9, 10, 14, 11, 15, 6, 7, 14, 9, 13, 15,};
        f = new FIFO(3);
        l = new LRU(3);
        se = new SecondChance(3);
        for (int i : reference) {
            System.out.println("Customer ID: " + i);
            f.add(i);
            l.add(i);
            se.add(i);
            f.print();
            l.print();
            se.print();
        }*/


        try {
            Socket s = new Socket("localhost", 8080);

            t1 = new Thread(new ServerHandler(s));
            t2 = new Thread(new AlgoHandler());

            t1.start();
            t2.start();

            while (!t2.isInterrupted()) ;

            System.out.println("LRU:" + l.pageFault + ",FIFO:" + f.pageFault + ",Second-chance:" + se.pageFault);
            /*f.print();
            l.print();
            se.print();*/

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ServerHandler implements Runnable {
        private final Socket serverSocket;

        public ServerHandler(Socket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            try {
                DataInputStream din = new DataInputStream(serverSocket.getInputStream());
                int inp;
                while (true) {
                    inp = din.readInt();
                    if (inp == 0) {
                        t2.interrupt();
                        break;
                    }

                    pc.produce(inp);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class AlgoHandler implements Runnable {
        public void run() {
            int n, inp;
            try {
                n = pc.consume();
                f = new FIFO(n);
                l = new LRU(n);
                se = new SecondChance(n);
                while (true) {
                    inp = pc.consume();
                    f.add(inp);
                    l.add(inp);
                    se.add(inp);
                    System.out.println("New Customer ID: " + inp);
                    f.print(true);
                    l.print(true);
                    se.print(true);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static class PC {
        LinkedList<Integer> list = new LinkedList<>();
        int capacity = 5;

        public void produce(int newValue) throws InterruptedException {
            synchronized (this) {
                while (list.size() == capacity) wait();
                list.add(newValue);
                notify();
            }
        }

        public int consume() throws InterruptedException {
            synchronized (this) {
                while (list.size() == 0) wait();
                int firstVal = list.removeFirst();
                notify();
                return firstVal;
            }
        }
    }
}


/*
13 12 10 11 12 10 12 6 12 15 8 9 12 12 6 5 14 11 8 7 11 10 10 13 15 11 6 11 8 12 14
*/

/*
12 11 6 6 15 12 10 8 13 9 10 15 14 9 5 14 14 13 6 8 7 9 5 6 13 15 7 9 10 14 11 15 6 7 14 9 13 15
*/


