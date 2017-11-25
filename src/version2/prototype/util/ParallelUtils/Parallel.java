package version2.prototype.util.ParallelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.rits.cloning.Cloner;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.util.EASTWebCloner;

public class Parallel {
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static final Cloner myCloner = EASTWebCloner.GetClonerInstance();
    //    private static ThreadPoolExecutor forPool = new ThreadPoolExecutor(NUM_CORES * 2, Integer.MAX_VALUE, 1l, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
    //            new NamedThreadFactory("Parallel.For", false));
    //    private static final ExecutorService forPool = Executors.newFixedThreadPool(NUM_CORES * 2, new NamedThreadFactory("Parallel.For", false));

    public static <E,R> List<Future<R>> ForEach(Config configInstance, final List<E> elements, final Operation<E,R> operation) {
        ThreadPoolExecutor forPool = new ThreadPoolExecutor(NUM_CORES, Integer.MAX_VALUE, 1l, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory(configInstance, "Parallel.For", false));
        List<Future<R>> futures = new ArrayList<Future<R>>();
        // invokeAll blocks for us until all submitted tasks in the call complete
        try {
            //            forPool.invokeAll(createCallables(elements, operation));

            List<E> myList = new ArrayList<E>(elements.size());

            for(E ele : elements) {
                myList.add(myCloner.deepClone(ele));
            }

            Collection<Callable<R>> runners = createCallables(elements, operation);
            forPool.allowCoreThreadTimeOut(true);
            futures = forPool.invokeAll(runners);
            forPool.shutdown();
            //            forPool.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            ErrorLog.add(Config.getInstance(), "Parallel.ForEach error during custom parallelized for each method.", e);
        }
        return futures;
    }

    private static <E,R> Collection<Callable<R>> createCallables(final Iterable<E> elements, final Operation<E,R> operation) {
        List<Callable<R>> callables = new LinkedList<Callable<R>>();
        for (final E elem : elements) {
            callables.add(new Callable<R>() {
                @Override
                public R call() {
                    return operation.perform(elem);
                }
            });
        }

        return callables;
    }

    public static interface Operation<E,R> {
        public R perform(E pParameter);
    }
}
