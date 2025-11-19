package com.ccinfom.ui.common;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Convenience wrapper around {@link SwingWorker} so forms can run blocking
 * service calls without duplicating boilerplate. The runner updates a
 * {@link StatusPanel} and forwards success/error callbacks on the EDT.
 */
public final class UiTaskRunner {

    private UiTaskRunner() {
        // Utility class
    }

    public static <T> void run(StatusPanel statusPanel,
                               String inProgressMessage,
                               String successMessage,
                               Callable<T> backgroundTask,
                               Consumer<T> onSuccess,
                               Consumer<Throwable> onFailure) {
        Objects.requireNonNull(statusPanel, "statusPanel");
        Objects.requireNonNull(backgroundTask, "backgroundTask");

        Consumer<T> successHandler = onSuccess != null ? onSuccess : value -> { };
        Consumer<Throwable> failureHandler = onFailure != null ? onFailure : UiTaskRunner::logUncaught;

        statusPanel.setInfo(inProgressMessage);

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return backgroundTask.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    if (successMessage != null && !successMessage.isBlank()) {
                        statusPanel.setSuccess(successMessage);
                    }
                    successHandler.accept(result);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    handleFailure(ie);
                } catch (ExecutionException ee) {
                    handleFailure(ee.getCause() != null ? ee.getCause() : ee);
                }
            }

            private void handleFailure(Throwable throwable) {
                String message = throwable.getMessage() == null
                        ? "Operation failed"
                        : throwable.getMessage();
                statusPanel.setError(message);
                if (SwingUtilities.isEventDispatchThread()) {
                    failureHandler.accept(throwable);
                } else {
                    SwingUtilities.invokeLater(() -> failureHandler.accept(throwable));
                }
            }
        };

        worker.execute();
    }

    private static void logUncaught(Throwable throwable) {
        throwable.printStackTrace();
    }

public static <T> void runAsync(
        String inProgressMessage,
        StatusPanel statusPanel,
        Callable<T> backgroundTask,
        Consumer<T> onSuccess
) {
    run(statusPanel, inProgressMessage, null, backgroundTask, onSuccess, null);
}

/** Simple version for Runnable tasks (no result handling). */
public static void runAsync(StatusPanel statusPanel, Runnable task) {
    run(statusPanel, "Processing...", null,
        () -> { task.run(); return null; },
        null,
        null
    );
}


}
