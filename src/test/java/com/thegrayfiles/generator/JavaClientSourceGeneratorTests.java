package com.thegrayfiles.generator;

import com.thegrayfiles.method.MethodParameter;
import com.thegrayfiles.method.MethodSignature;
import com.thegrayfiles.server.ServerEndpoint;
import com.thegrayfiles.server.ServerPathVariable;
import com.thegrayfiles.server.ServerRequestParameter;
import com.thegrayfiles.util.InverseSpringControllerAnnotationProcessor;
import com.thegrayfiles.util.TestSourceGenerator;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.thegrayfiles.builders.ClientStubBuilder.aStub;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class JavaClientSourceGeneratorTests {

    private static final String GENERATED_SOURCES_DIR = "/code/github/spring-mvc-annotation-processor/target/generated-sources";
    private static final String TEST_CLASSES_DIR = "/code/github/spring-mvc-annotation-processor/target/test-classes";
    private File testClassesDirectory;
    private File generatedSource;

    @BeforeMethod
    public void setup() throws IOException {
        File generatedSourcesDirectory = new File(GENERATED_SOURCES_DIR);
        generatedSourcesDirectory.mkdirs();

        testClassesDirectory = new File(TEST_CLASSES_DIR);
        testClassesDirectory.mkdirs();

        generatedSource = File.createTempFile("ClientStub", ".java", generatedSourcesDirectory);
        generatedSource.deleteOnExit();
    }

    @Test
    public void canProcessRequestMappingWithVoidReturnType() throws IOException {
        ServerEndpoint stub = aStub().thatReturns(void.class).build();
        canProcessRequestMapping(stub);
    }

    @Test
    public void canProcessRequestMappingWithNonPrimitiveReturnType() throws IOException {
        ServerEndpoint stub = aStub().thatReturns(Integer.class).build();
        canProcessRequestMapping(stub);
    }

    @Test
    public void canProcessRequestMappingWithPrimitiveReturnType() throws IOException {
        ServerEndpoint stub = aStub().thatReturns(int.class).build();
        canProcessRequestMapping(stub);
    }

    @Test
    public void canProcessRequestMappingWithMultipleRequestParameters() throws IOException {
        ServerEndpoint endpoint = aStub()
                .withRequestParam(new ServerRequestParameter(String.class, "param"))
                .withRequestParam(new ServerRequestParameter(Integer.class, "anotherparam"))
                .withRequestParam(new ServerRequestParameter(Integer.class, "onemoreofthesametype")).build();
        canProcessRequestMapping(endpoint);
    }

    @Test
    public void canProcessRequestMappingWithSinglePathVariable() throws IOException {
        ServerEndpoint endpoint = aStub().withPathVariable(new ServerPathVariable(String.class, "somekindofpathvariable")).build();
        canProcessRequestMapping(endpoint);
    }

    private void canProcessRequestMapping(ServerEndpoint stub) throws IOException {
        MethodImplementationSourceGenerator clientGenerator = new TestSourceGenerator();
        JavaClientSourceGenerator generator = new JavaClientSourceGenerator(clientGenerator, generatedSource);

        generator.addEndpoint(stub);

        // this should produce the client stub...
        generator.process();

        // file size should have increased
        assertTrue(FileUtils.sizeOf(generatedSource) != 0, "Client file size did not increase.");

        // inverse process the java source to extract the method signatures
        InverseSpringControllerAnnotationProcessor inverseProcessor = new InverseSpringControllerAnnotationProcessor(generatedSource, testClassesDirectory);

        inverseProcessor.process();

        List<MethodSignature> inverseProcessorMethodSignatures = inverseProcessor.getMethodSignatures();
        assertEquals(inverseProcessorMethodSignatures.size(), 1, "Expected a single method signature in inverse processor.");
        MethodSignature inverseProcessorMethodSignature = inverseProcessorMethodSignatures.get(0);
        assertMethodSignaturesAreSame(inverseProcessorMethodSignature, stub.getMethodSignature());
        assertEquals(inverseProcessorMethodSignature.getParameters().size(), stub.getRequestParameters().size() + stub.getPathVariables().size(), "Number of method parameters differ from number of request parameters.");

        Map<Class<?>, Integer> stubTypeCount = getStubTypeCount(stub);
        Map<Class<?>, Integer> methodParamTypeCount = getMethodParameterTypeCount(inverseProcessorMethodSignature);
        Set<Class<?>> stubParamTypes = stubTypeCount.keySet();
        assertEquals(stubParamTypes, methodParamTypeCount.keySet());
        for (Class<?> requestParamType : stubParamTypes) {
            assertTrue(methodParamTypeCount.containsKey(requestParamType));
            assertEquals(stubTypeCount.get(requestParamType), methodParamTypeCount.get(requestParamType));
        }
    }

    private Map<Class<?>, Integer> getMethodParameterTypeCount(MethodSignature methodSignature) {
        Map<Class<?>, Integer> typeCount = new HashMap<Class<?>, Integer>();
        for (MethodParameter parameter : methodSignature.getParameters()) {
            Class<?> type = parameter.getType();
            incrementTypeCount(typeCount, type);
        }
        return typeCount;
    }

    private void incrementTypeCount(Map<Class<?>, Integer> typeCount, Class<?> type) {
        if (typeCount.containsKey(type)) {
            typeCount.put(type, typeCount.get(type) + 1);
        } else {
            typeCount.put(type, 1);
        }
    }

    private Map<Class<?>, Integer> getStubTypeCount(ServerEndpoint stub) {
        Map<Class<?>, Integer> typeCount = new HashMap<Class<?>, Integer>();
        for (ServerRequestParameter parameter : stub.getRequestParameters()) {
            incrementTypeCount(typeCount, parameter.getType());
        }
        for (ServerPathVariable pathVariable : stub.getPathVariables()) {
            incrementTypeCount(typeCount, pathVariable.getType());
        }
        return typeCount;
    }

    private void assertMethodSignaturesAreSame(MethodSignature actual, MethodSignature expected) {
        assertEquals(actual.getReturnType(), expected.getReturnType(), "Method signature return types differ.");
        assertEquals(actual.getMethodName(), expected.getMethodName(), "Method names differ.");
    }
}