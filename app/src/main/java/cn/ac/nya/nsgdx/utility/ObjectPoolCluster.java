package cn.ac.nya.nsgdx.utility;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Created by drzzm on 2018.2.14.
 */
public class ObjectPoolCluster {

    public class ObjectPool {

        private int counter;
        private LinkedList<IObject> objectPool;
        private LinkedList<IObject> cachePool;
        private Runnable runnable;
        private final ReentrantLock lock = new ReentrantLock();

        public ObjectPool() {
            counter = 0;
            objectPool = new LinkedList<>();
            cachePool = new LinkedList<>();
            this.runnable = () -> {
                lock.lock();

                cachePool.clear();
                cachePool.addAll(objectPool);
                for (IObject i : cachePool) {
                    if (i.onUpdate(counter) == IObject.Result.END) {
                        objectPool.remove(i);
                    }
                }
                counter += 1;

                lock.unlock();
            };
        }

        public void render(Renderer renderer) {
            lock.lock();

            renderer.begin();
            for (IObject i : objectPool)
                i.onRender(renderer);
            renderer.end();

            lock.unlock();
        }

        public void add(IObject object) {
            lock.lock();
            objectPool.add(object);
            lock.unlock();
        }

        public void add(IObject[] objects) {
            lock.lock();
            objectPool.addAll(Arrays.asList(objects));
            lock.unlock();
        }

        public void clear() {
            lock.lock();
            objectPool.clear();
            lock.unlock();
        }

        public int size() {
            try {
                lock.lock();
                return objectPool.size();
            } finally {
                lock.unlock();
            }
        }

        public void removeLast() {
            lock.lock();
            objectPool.removeLast();
            lock.unlock();
        }

        public IObject getLast() {
            try {
                lock.lock();
                return objectPool.getLast();
            } finally {
                lock.unlock();
            }
        }

    }

    private ScheduledThreadPoolExecutor poolExecutor;
    private LinkedList<ObjectPool> poolCluster;
    private LinkedHashMap<ObjectPool, ScheduledFuture> poolTask;
    private int poolSize;
    private int capacity;

    public ObjectPoolCluster(int poolSize, int capacity) {
        poolExecutor = new ScheduledThreadPoolExecutor(capacity);
        poolCluster = new LinkedList<>();
        poolTask = new LinkedHashMap<>();
        this.poolSize = poolSize;
        this.capacity = capacity;

        addPool();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("poolCluster info {\n");
        builder.append("    ");
        builder.append("pool count: ");
        builder.append(poolCluster.size());
        builder.append("\n");
        builder.append("    ");
        builder.append("thread count: ");
        builder.append(poolExecutor.getActiveCount());
        builder.append("\n");
        builder.append("    ");
        builder.append("queue count: ");
        builder.append(poolExecutor.getQueue().size());
        builder.append("\n");
        builder.append("    ");
        builder.append("obj count: ");
        for (ObjectPool i : poolCluster) {
            builder.append(i.size());
            builder.append("    ");
        }
        builder.append("\n");
        builder.append("    ");
        builder.append("obj sum: ");
        int sum = 0;
        for (ObjectPool i : poolCluster) sum += i.size();
        builder.append(sum);
        builder.append("\n}");

        return builder.toString();
    }

    private void addPool() {
        ObjectPool pool = new ObjectPool();
        poolCluster.add(pool);
        poolTask.put(pool, poolExecutor.scheduleWithFixedDelay(
            pool.runnable, 10, 10, TimeUnit.MILLISECONDS
        ));
    }

    private void removePool(ObjectPool pool) {
        if (!poolTask.containsKey(pool)) return;
        RunnableScheduledFuture task = (RunnableScheduledFuture) poolTask.get(pool);
        poolExecutor.remove(task);
        poolCluster.remove(pool);
        poolTask.remove(pool);
    }

    public ObjectPool first() {
        return poolCluster.getFirst();
    }

    public ObjectPool last() {
        return poolCluster.getLast();
    }

    public void add(IObject object) {
        first().add(object);
    }

    public void add(IObject[] objects) {
        first().add(objects);
    }

    public void balance() {
        if (first().size() > poolSize && poolCluster.size() < capacity) {
            addPool();
        } else if (first().size() == 0 && poolCluster.size() > 1) {
            first().clear();
            removePool(first());
        } else {
            int size = (last().size() - first().size()) / 2;
            for (int i = 0; i < size; i++) {
                first().add(last().getLast());
                last().removeLast();
            }
        }
       doSort();
    }

    private void doSort() {
        ObjectPool[] objects = poolCluster.toArray(new ObjectPool[]{});
        Arrays.sort(objects, (a, b) -> a.size() - b.size());
        ListIterator<ObjectPool> it = this.poolCluster.listIterator();
        for (ObjectPool e : objects) {
            it.next(); it.set(e);
        }
    }

    public void clear() {
        for (ObjectPool i : poolCluster)
            i.clear();
    }

    public void close() {
        clear();
        poolExecutor.shutdown();
    }

    public void render(Renderer renderer) {
        for (ObjectPool i : poolCluster)
            i.render(renderer);
    }

    public IObject[] toArray() {
        LinkedList<IObject> list = new LinkedList<>();
        for (ObjectPool i : poolCluster)
            list.addAll(i.objectPool);
        return (IObject[]) list.toArray();
    }

}
