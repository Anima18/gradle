/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.logging.internal;

import org.gradle.api.logging.LogLevel;

import java.util.LinkedList;

/**
 * An {@code org.gradle.logging.internal.OutputEventListener} implementation which generates output events to log the
 * progress of operations.
 */
public class ProgressLogEventGenerator implements OutputEventListener {
    public static final String EOL = System.getProperty("line.separator");
    private final OutputEventListener listener;
    private final boolean deferHeader;
    private final LinkedList<Operation> operations = new LinkedList<Operation>();

    public ProgressLogEventGenerator(OutputEventListener listener, boolean deferHeader) {
        this.listener = listener;
        this.deferHeader = deferHeader;
    }

    public void onOutput(OutputEvent event) {
        if (event instanceof ProgressStartEvent) {
            ProgressStartEvent progressStartEvent = (ProgressStartEvent) event;
            Operation operation = new Operation();
            operation.category = progressStartEvent.getCategory();
            operation.description = progressStartEvent.getDescription();
            operation.status = "";
            operations.add(operation);
            onStart(operation);
        } else if (event instanceof ProgressCompleteEvent) {
            assert !operations.isEmpty();
            ProgressCompleteEvent progressCompleteEvent = (ProgressCompleteEvent) event;
            Operation operation = operations.removeLast();
            operation.status = progressCompleteEvent.getStatus();
            onComplete(operation);
        } else if (event instanceof RenderableOutputEvent) {
            doOutput((RenderableOutputEvent) event);
        } else if (!(event instanceof ProgressEvent)) {
            listener.onOutput(event);
        }
    }

    private void doOutput(RenderableOutputEvent event) {
        for (Operation operation : operations) {
            operation.completeHeader();
        }
        listener.onOutput(event);
    }

    private void onComplete(Operation operation) {
        operation.complete();
    }

    private void onStart(Operation operation) {
        if (!deferHeader) {
            operation.startHeader();
        }
    }

    enum State {None, HeaderStarted, HeaderCompleted, Completed}

    private class Operation {
        private String category;
        private String description;
        private String status;
        private State state = State.None;

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }

        private void doOutput(RenderableOutputEvent event) {
            for (Operation pending : operations) {
                if (pending == this) {
                    break;
                }
                pending.completeHeader();
            }
            listener.onOutput(event);
        }

        public void startHeader() {
            assert state == State.None;
            boolean hasDescription = description.length() > 0;
            if (hasDescription) {
                state = State.HeaderStarted;
                doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, description));
            } else {
                state = State.HeaderCompleted;
            }
        }

        public void completeHeader() {
            boolean hasDescription = description.length() > 0;
            switch (state) {
                case None:
                    if (hasDescription) {
                        listener.onOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, description + EOL));
                    }
                    break;
                case HeaderStarted:
                    listener.onOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, EOL));
                    break;
                case HeaderCompleted:
                    return;
                default:
                    throw new IllegalStateException();
            }
            state = State.HeaderCompleted;
        }

        public void complete() {
            boolean hasStatus = status.length() > 0;
            boolean hasDescription = description.length() > 0;
            switch (state) {
                case None:
                    if (hasDescription && hasStatus) {
                        doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, description + ' ' + status + EOL));
                    } else if (hasDescription) {
                        doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, description + EOL));
                    } else if (hasStatus) {
                        doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, status + EOL));
                    }
                    break;
                case HeaderStarted:
                    if (hasStatus) {
                        doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, ' ' + status + EOL));
                    } else {
                        doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, EOL));
                    }
                    break;
                case HeaderCompleted:
                    if (hasDescription && hasStatus) {
                        doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, description + ' ' + status + EOL));
                    } else if (hasStatus) {
                        doOutput(new StyledTextOutputEvent(0, category, LogLevel.LIFECYCLE, status + EOL));
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
            state = State.Completed;
        }
    }
}
