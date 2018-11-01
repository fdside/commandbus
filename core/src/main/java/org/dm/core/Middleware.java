package org.dm.core;

import java.util.function.Function;

/**
 * <em>Middleware</em> is used for all kind of
 * <em>Command</em> pre/post processing/transformation
 * Typical use case for <em>Middleware</em> would be:
 * <ul>
 * <li>Start transactional context</li>
 * <li>Log</li>
 * <li>Measure execution time</li>
 * </ul>
 *
 * <p> <b>Please, don't forget to call next.apply(command)</b>
 * otherwise, <em>Command</em> won't be passed further and
 * won't reach other <em>Middleware</em> and <em>CommandHandler</em>
 *
 * <p> Having next.apply(command) in control allows to
 * make pre-processing/post-processing or just stop the whole <em>Command</em>
 * execution, by not calling next.apply(command)
 *
 * @implSpec <pre> {@code
 *   Timer.start();
 *   R result = next.apply(command);
 *   Timer.stop();
 *   return result;
 *   }</pre>
 */

public interface Middleware {
    <R> R execute(Object command, Function<Object, R> next);
}
