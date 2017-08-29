/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.tests.sampled;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AllGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.sampled.callgraph.ProfilingCallGraphAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.junit.Test;

/**
 * Test profiling data where the input are stack traces from events
 *
 * @author Geneviève Bastien
 */
public class SampledCallGraphTest {

    private static final long @NonNull [] CALLSITE_1 = { 1, 2, 3, 4 };
    private static final long @NonNull [] CALLSITE_2 = { 1, 2, 3 };
    private static final long @NonNull [] CALLSITE_3 = { 1, 2, 3, 4 };
    private static final long @NonNull [] CALLSITE_4 = { 1, 3, 4 };
    private static final long @NonNull [] CALLSITE_5 = { 1, 2, 5 };
    private static final long @NonNull [] CALLSITE_6 = { 1, 2, 5, 4 };
    private static final long @NonNull [] CALLSITE_7 = { 10, 11, 12 };
    private static final long @NonNull [] CALLSITE_8 = { 10, 11 };
    private static final long @NonNull [] CALLSITE_9 = { 1, 2, 3, 4 };
    private static final long @NonNull [] CALLSITE_10 = { 1, 2, 4, 5 };

    /**
     * A default implementation of the profiling call graph analysis for test
     * purposes
     */
    private static class TestProfilingAnalysis extends ProfilingCallGraphAnalysisModule {

        private final @NonNull ICallStackElement fOneElement;

        public TestProfilingAnalysis() {
            ICallStackElement element = new CallStackElement("test", AllGroupDescriptor.getInstance());
            addRootElement(element);
            fOneElement = element;
        }

        public @NonNull ICallStackElement getElement() {
            return fOneElement;
        }

        @Override
        public Collection<ICallStackGroupDescriptor> getGroupDescriptors() {
            return Collections.singleton(AllGroupDescriptor.getInstance());
        }

        @Override
        public Map<String, Collection<Object>> getCallStack(@NonNull ITmfEvent event) {
            return Collections.emptyMap();
        }

        @Override
        protected void processEvent(@NonNull ITmfEvent event) {
            // Nothing to do here
        }

    }

    /**
     * Test a full sampling for one group
     */
    @Test
    public void testStackTraces() {
        TestProfilingAnalysis pg = new TestProfilingAnalysis();
        try {
            ICallStackElement element = pg.getElement();
            pg.addStackTrace(element, CALLSITE_1, 1);
            pg.addStackTrace(element, CALLSITE_2, 2);
            pg.addStackTrace(element, CALLSITE_3, 3);
            pg.addStackTrace(element, CALLSITE_4, 4);
            pg.addStackTrace(element, CALLSITE_5, 5);
            pg.addStackTrace(element, CALLSITE_6, 6);
            pg.addStackTrace(element, CALLSITE_7, 7);
            pg.addStackTrace(element, CALLSITE_8, 8);
            pg.addStackTrace(element, CALLSITE_9, 9);
            pg.addStackTrace(element, CALLSITE_10, 10);

            Collection<AggregatedCallSite> aggregatedData = pg.getCallingContextTree(element);
            assertNotNull(aggregatedData);
            assertEquals(2, aggregatedData.size());

            for (AggregatedCallSite callsite : aggregatedData) {
                switch (callsite.getSymbol().resolve(Collections.emptySet())) {
                case "0x1": {
                    assertEquals(8, callsite.getLength());
                    assertEquals(2, callsite.getCallees().size());
                    for (AggregatedCallSite childCallsite : callsite.getCallees()) {
                        switch (childCallsite.getSymbol().resolve(Collections.emptySet())) {
                        case "0x2":
                            assertEquals(7, childCallsite.getLength());
                            assertEquals(3, childCallsite.getCallees().size());
                            break;
                        case "0x3":
                            assertEquals(1, childCallsite.getLength());
                            assertEquals(1, childCallsite.getCallees().size());
                            break;
                        default:
                            throw new IllegalStateException("Unknown callsite: " + childCallsite.getSymbol());
                        }
                    }
                }
                    break;
                case "0xa": {
                    assertEquals(2, callsite.getLength());
                    assertEquals(1, callsite.getCallees().size());
                    AggregatedCallSite childCallsite = callsite.getCallees().iterator().next();
                    assertEquals(2, childCallsite.getLength());
                    assertEquals(1, callsite.getCallees().size());
                }
                    break;
                default:
                    throw new IllegalStateException("Unknown callsite: " + callsite.getSymbol());
                }
            }
        } finally {
            pg.dispose();
        }

    }

}