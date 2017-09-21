import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.nio.*;
import java.nio.file.*;



/**
* A multithreaded implementation of producer/consumer queue.
* 
* A two level counter is used for logging.
* 
* @author wayneng
*
*/
public class MTQ {
    
    public static void p(String f, Object ...o) {
        System.out.printf(f, o);
    }

    public static void pl(Object o) {
        System.out.println(o);
    }

    class Producer implements Runnable {

        String id = null;
        MTQueue q = null;
        Lock lock = null;
        Utils utils = null;
        Logger logger = null;
        AtomicInteger aint = null;
        int MOD = 10000;
        int sizeString = 20;
        int minSleep = 1;
        int maxSleep = 2;
        int minOp = 1;
        int maxOp = 5;

        public Producer(String id, MTQueue q, Lock lock, Logger logger) {
            this.id = id;
            this.q = q;
            this.lock = lock;
            this.utils = new Utils();
            this.logger = logger;
            this.aint = new AtomicInteger(0);
            this.logger.addId(id);
        }

        @Override
        public void run() {
            while(true) {
                produce();
                try {
                    int ms = utils.getInt(minSleep, maxSleep);
                    Thread.sleep(ms);
                } catch(Exception e) {
                    
                }
            }
        }

        public void produce() {
            String s = utils.getString(sizeString);
            String item = longOp(s);
            q.put(item, id);
            aint.incrementAndGet();
            if(aint.get() % MOD == 0) {
                String msg = String.format(
                    "%s produced %d, last str: %s", id, MOD, item);
                logger.appendLog(msg);
            }
            if(aint.get() % MOD == 0) {
                logger.incId(id);
            }
        }
        
        private String longOp(String s) {
            int numRotations = utils.getInt(minOp, maxOp);
            char [] a = s.toCharArray();
            int size = s.length();
            
            for(int i = 0; i < numRotations; i++) {
                char cfirst = a[0];
                for(int j = 1; j < size; j++) {
                    a[j-1] = a[j];
                }
                a[size-1] = cfirst;
            }
            
            return new String(a);
        }
    }
    
    class Consumer implements Runnable {

        String id = null;
        MTQueue q = null;
        Lock lock = null;
        Utils utils = null;
        Logger logger = null;
        AtomicInteger aint = null;
        int MOD = 10000;
        int minSleep = 1;
        int maxSleep = 2;
        int minOp = 1;
        int maxOp = 10;

        public Consumer(String id, MTQueue q, Lock lock, Logger logger) {
            this.id = id;
            this.q = q;
            this.lock = lock;
            this.utils = new Utils();
            this.logger = logger;
            this.aint = new AtomicInteger(0);
            this.logger.addId(id);
        }

        @Override
        public void run() {
            while(true) {
                consume();
                try {
                    int ms = utils.getInt(minSleep, maxSleep);
                    Thread.sleep(ms);
                } catch(Exception e) {
                    
                }
            }
        }

        public void consume() {
            String item = q.get(id);
            String s = longOp(item);
            aint.incrementAndGet();
            if(aint.get() % MOD == 0) {
                String msg = String.format(
                    "%s consumed %d, last str: %s", id, MOD, s);
                logger.appendLog(msg);
            }
            if(aint.get() % MOD == 0) {
                logger.incId(id);
            }
        }
        
        private String longOp(String item) {
            char [] a = item.toCharArray();
            AtomicInteger ctr = new AtomicInteger(0);
            longOp(a, 0, ctr);
            return item;
        }
        
        private void longOp(char [] a, int idx, AtomicInteger ctr) {
            if(idx >= a.length)
                return;
            ctr.incrementAndGet();
            if(ctr.get() > maxOp) {
            	return;
            }
            for(int i = idx; i < a.length; i++) {
                longOp(a, i+1, ctr);
            }
        }
    }

    class MTQueue {
        
        Condition conditionRd = null;
        Condition conditionWr = null;
        ReentrantLock lock = null;
        int maxSizeQ = 10000;
        int size = 0;
        final static int defaultTime = 60000;
        LinkedList<String> list = null;
        Logger logger = null;
        AtomicInteger aintPut = null;
        AtomicInteger aintGet = null;
        String id = "mtqueue";
        int MOD = 5000;

        public MTQueue(int maxSizeQ, Logger logger) {
            this.maxSizeQ = (10 < maxSizeQ && maxSizeQ < 100000) ? 
                maxSizeQ : this.maxSizeQ;
            list = new LinkedList<>();
            lock = new ReentrantLock();
            conditionRd = lock.newCondition();
            conditionWr = lock.newCondition();
            this.aintPut = new AtomicInteger(0);
            this.aintGet = new AtomicInteger(0);
            this.logger = logger;
            logger.addId(id);
        }

        public synchronized int getCurrentSize() {
            return size;
        }

        public void put(String item, String id) {
            put(item, defaultTime, id);
        }

        public String get(String id) {
            return get(defaultTime, id);
        }
        
        public void put(String item, long expire, String id) {
            try {
                lock.lock();
                while(size == maxSizeQ) {
                    conditionWr.await(expire, TimeUnit.MILLISECONDS);
                }
                list.add(item);
                size++;
                conditionRd.signalAll();
                aintPut.incrementAndGet();
                
                if(aintPut.get() % MOD == 0) {
                    String msg = String.format("%s add %d puts", id, MOD);
                    logger.appendLog(msg);  
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

        public String get(long expire, String id) {
            try {
                lock.lock();
                while(size == 0) {
                    conditionRd.await(expire, TimeUnit.MILLISECONDS);
                }
                String item = list.removeFirst();
                size--;
                conditionWr.signalAll();
                aintGet.incrementAndGet();
                if(aintGet.get() % MOD == 0) {
                    String msg = String.format("%s add %d gets", id, MOD);
                    logger.appendLog(msg);  
                }
                return item;
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
            return null;
        }
    }

    class Logger {

        class LogRunnable implements Runnable {
            @Override
            public void run() {
                try {
                    FileWriter fw = new FileWriter(file, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    pw = new PrintWriter(bw, true);

                    while(true) {
                        try {
                            Thread.sleep(logIntervalMS);
                            long time = System.currentTimeMillis();
                            String s = String.format("logging time %d", time);
                            
                            try {
                                lock.lock();
                                if(pw != null && msgbuffer.size() != 0) {
                                    pw.println(s);
                                    for(String msg: msgbuffer) {
                                        pw.println(msg);
                                    }
                                    msgbuffer.clear();
                                }
                            } finally {
                                lock.unlock();
                                //pl("appended log");
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                
                } catch(IOException e) {
                    
                } finally {
                    if(pw != null) {
                        pw.close();
                        pw = null;
                    }
                }
                
            }
        }

        String filename = "output.log";
        int logIntervalMS = 60000;
        final static int logIntervalDefault = 60000;
        File file = null;
        PrintWriter pw = null;
        ConcurrentHashMap<String, AtomicInteger> map;
        List<String> msgbuffer = null;
        ReentrantLock lock = null;
        
        public Logger() {
            this("output.log", logIntervalDefault);
        }

        public Logger(String filename, int logIntervalMS) {
            this.filename = filename;
            this.logIntervalMS = logIntervalMS;
            this.file = new File(filename);
            this.map = new ConcurrentHashMap<>();
            this.msgbuffer = new ArrayList<>();
            this.lock = new ReentrantLock();
        }

        public synchronized void appendLog(String msg) {
            try {
                lock.lock();
                msgbuffer.add(msg);
            } finally {
                lock.unlock();
            }
        }
        
        public void addId(String id) {
            if(map.containsKey(id))
                return;
            map.put(id, new AtomicInteger(0));
        }
        
        public synchronized void incId(String id) {
            if(!map.containsKey(id)) {
                return;
            }
            AtomicInteger aint = map.get(id);
            aint.incrementAndGet();
        }
        
        public synchronized void setId(String id, int val) {
            if(!map.containsKey(id)) {
                return;
            }
            AtomicInteger aint = map.get(id);
            aint.set(val);
        }
        
        public void start() {
            LogRunnable logRunnable = new LogRunnable();
            Thread thread = new Thread(logRunnable);
            thread.start();
        }
    }

    Utils utils = null;
    ReentrantLock lockProducer = null;
    ReentrantLock lockConsumer = null;
    int targetPercentUsageCPU = 75;
    int maxSizeQ = 10000;
    int maxThreadsProduce = 500;
    int maxThreadsConsume = 500;
    int numThreadsProduce = 10;
    int numThreadsConsume = 10;

    public MTQ() {
        this(10000, 75);
    }

    public MTQ(
        int maxSizeQ, 
        int targetPercentUsageCPU) 
    {
        if(10 < maxSizeQ && maxSizeQ < 100000)
            this.maxSizeQ = maxSizeQ;
        if(10 < targetPercentUsageCPU && targetPercentUsageCPU < 90)
            this.targetPercentUsageCPU = targetPercentUsageCPU;
        utils = new Utils();
        lockProducer = new ReentrantLock();
        lockConsumer = new ReentrantLock();
    }
    
    public void start() {
        List<Thread> listProducers = new ArrayList<>();
        List<Thread> listConsumers = new ArrayList<>();
        Logger logger = new Logger();
        MTQueue queue = new MTQueue(10000, logger);

        boolean debug = true;
        
        if(debug) {
            logger.start();
        }
        
        for(int i = 0; i < numThreadsProduce; i++) {
            String id = String.format("producer_%03d", i);
            Thread t = new Thread(new Producer(id, queue, lockProducer, logger));
            listProducers.add(t);
        }

        for(int i = 0; i < numThreadsConsume; i++) {
            String id = String.format("consumer_%03d", i);
            Thread t = new Thread(new Consumer(id, queue, lockConsumer, logger));
            listConsumers.add(t);
        }

        for(Thread t: listProducers) {
            t.start();
        }

        for(Thread t: listConsumers) {
            t.start();
        }
    }
}