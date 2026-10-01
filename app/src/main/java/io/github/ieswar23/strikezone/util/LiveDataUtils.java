package io.github.ieswar23.strikezone.util;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

/** Small Java-friendly LiveData operators (avoids Kotlin lambda interop in Transformations). */
public final class LiveDataUtils {

    public interface Mapper<I, O> {
        O apply(I input);
    }

    public interface Combiner<A, B, O> {
        O apply(A a, B b);
    }

    private LiveDataUtils() {
    }

    public static <I, O> LiveData<O> map(LiveData<I> source, Mapper<I, O> mapper) {
        MediatorLiveData<O> result = new MediatorLiveData<>();
        result.addSource(source, value -> result.setValue(mapper.apply(value)));
        return result;
    }

    public static <I, O> LiveData<O> switchMap(LiveData<I> source, Mapper<I, LiveData<O>> mapper) {
        MediatorLiveData<O> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<I>() {
            private LiveData<O> current;

            @Override
            public void onChanged(I value) {
                LiveData<O> next = mapper.apply(value);
                if (current == next) return;
                if (current != null) result.removeSource(current);
                current = next;
                if (current != null) result.addSource(current, result::setValue);
                else result.setValue(null);
            }
        });
        return result;
    }

    /** Emits whenever either source changes (values may be null until both have emitted). */
    public static <A, B, O> LiveData<O> combine(LiveData<A> a, LiveData<B> b, Combiner<A, B, O> combiner) {
        MediatorLiveData<O> result = new MediatorLiveData<>();
        result.addSource(a, value -> result.setValue(combiner.apply(value, b.getValue())));
        result.addSource(b, value -> result.setValue(combiner.apply(a.getValue(), value)));
        return result;
    }
}
