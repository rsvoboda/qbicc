package org.qbicc.driver;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.context.Location;
import org.qbicc.graph.Node;
import org.qbicc.type.definition.element.Element;

public final class BaseDiagnosticContext implements DiagnosticContext  {
    final ConcurrentHashMap<AttachmentKey<?>, Object> attachmentsMap = new ConcurrentHashMap<AttachmentKey<?>, Object>();
    final ConcurrentLinkedDeque<Diagnostic> diagnostics = new ConcurrentLinkedDeque<Diagnostic>();
    final ConcurrentHashMap<String, String> stringCache = new ConcurrentHashMap<String, String>();
    final AtomicInteger errorCnt = new AtomicInteger(0);
    final AtomicInteger warnCnt = new AtomicInteger(0);

    public BaseDiagnosticContext() {
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment(final AttachmentKey<T> key) {
        return (T) attachmentsMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachmentOrDefault(final AttachmentKey<T> key, final T defVal) {
        return (T) attachmentsMap.getOrDefault(key, defVal);
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttachment(final AttachmentKey<T> key, final T value) {
        return (T) attachmentsMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttachmentIfAbsent(final AttachmentKey<T> key, final T value) {
        return (T) attachmentsMap.putIfAbsent(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttachment(final AttachmentKey<T> key) {
        return (T) attachmentsMap.remove(key);
    }

    public <T> boolean removeAttachment(final AttachmentKey<T> key, final T expect) {
        return attachmentsMap.remove(key, expect);
    }

    @SuppressWarnings("unchecked")
    public <T> T replaceAttachment(final AttachmentKey<T> key, final T update) {
        return (T) attachmentsMap.replace(key, update);
    }

    public <T> boolean replaceAttachment(final AttachmentKey<T> key, final T expect, final T update) {
        return attachmentsMap.replace(key, expect, update);
    }

    @SuppressWarnings("unchecked")
    public <T> T computeAttachmentIfAbsent(final AttachmentKey<T> key, final Supplier<T> function) {
        return (T) attachmentsMap.computeIfAbsent(key, k -> function.get());
    }

    @SuppressWarnings("unchecked")
    public <T> T computeAttachmentIfPresent(final AttachmentKey<T> key, final Function<T, T> function) {
        return (T) attachmentsMap.computeIfPresent(key, (k, v) -> function.apply((T) v));
    }

    @SuppressWarnings("unchecked")
    public <T> T computeAttachment(final AttachmentKey<T> key, final Function<T, T> function) {
        return (T) attachmentsMap.compute(key, (k, v) -> function.apply((T) v));
    }

    public int errors() {
        return errorCnt.get();
    }

    public int warnings() {
        return warnCnt.get();
    }

    Diagnostic msg(final Diagnostic diagnostic) {
        if (diagnostic.getParent() == null) {
            diagnostics.addLast(diagnostic);
            Diagnostic.Level level = diagnostic.getLevel();
            if (level == Diagnostic.Level.ERROR) {
                errorCnt.getAndIncrement();
            } else if (level == Diagnostic.Level.WARNING) {
                warnCnt.getAndIncrement();
            }
        }
        return diagnostic;
    }

    public Diagnostic msg(final Diagnostic parent, final Location loc, final Diagnostic.Level level, final String fmt, final Object... args) {
        return msg(new Diagnostic(parent, loc, level, fmt, args));
    }

    public Diagnostic msg(final Diagnostic parent, final Element element, final Node node, final Diagnostic.Level level, final String fmt, final Object... args) {
        Location loc;
        if (element == null && node == null) {
            loc = Location.NO_LOC;
        } else {
            Location.Builder lb = Location.builder();
            if (element != null) {
                lb.setElement(element);
            }
            if (node != null) {
                lb.setNode(node);
            }
            loc = lb.build();
        }
        return msg(parent, loc, level, fmt, args);
    }

    public Iterable<Diagnostic> getDiagnostics() {
        return Collections.unmodifiableCollection(diagnostics);
    }

    String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
        byte[] array = new byte[length];
        int pos = buffer.position();
        buffer.position(offset);
        try {
            buffer.get(array);
            return deduplicate(new String(array));
        } finally {
            buffer.position(pos);
        }
    }

    String deduplicate(final String original) {
        return stringCache.computeIfAbsent(original, Function.identity());
    }
}