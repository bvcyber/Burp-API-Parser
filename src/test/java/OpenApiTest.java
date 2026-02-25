import com.bureauveritas.modelparser.control.file.handler.openapi.OpenAPIFileHandler;
import com.bureauveritas.modelparser.control.file.handler.openapi.OpenAPIUtils;
import com.bureauveritas.modelparser.model.OpenModelFile;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.examples.ExampleGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OpenApiTest extends MontoyaTest{
    private final OpenModelFile loaderSampleOpenAPIExhaustive = new OpenModelFile();
//    private final OpenModelFile loaderSwaggerK8sOpenAPIV2 = new OpenModelFile();
//    private final OpenModelFile loaderAWSMediaConnectSwagger = new OpenModelFile();
    private final OpenModelFile loaderDigitalOceanSwagger = new OpenModelFile();
    private final OpenModelFile loaderAdyen = new OpenModelFile();

    private OpenAPI getUnresolvedModel(OpenModelFile loader) {
        return (OpenAPI) loader.getModelFileHandler().getAdditionalProperties().get(OpenAPIFileHandler.UNRESOLVED_MODEL);
    }

    private OpenAPI getModel(OpenModelFile loader) {
        return (OpenAPI) loader.getModelFileHandler().getModel();
    }

    @BeforeAll
    static void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(ExampleGenerator.class);
        logger.atLevel(Level.ERROR);
    }

    @Test
    void testOpenAPI2() {
        try {
            loaderSampleOpenAPIExhaustive.loadModelFromFile(new File(getClass().getResource("trading-api.json").toURI()));
            assertEquals("OpenAPIFileHandler", loaderSampleOpenAPIExhaustive.getModelFileHandler().toString());
            loaderSampleOpenAPIExhaustive.loadModelFromFile(new File(getClass().getResource("garbage.json").toURI()));
            assertEquals("ErrorFileHandler", loaderSampleOpenAPIExhaustive.getModelFileHandler().toString());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testOpenAPIExample(){
        try {
            loaderSampleOpenAPIExhaustive.loadModelFromFile(new File(getClass().getResource("sample-openapi-exhaustive.yaml").toURI()));
            assertEquals("OpenAPIFileHandler", loaderSampleOpenAPIExhaustive.getModelFileHandler().toString());
//            loaderSwaggerK8sOpenAPIV2.loadModelFromFile(new File(getClass().getResource("swagger-k8s-openapiv2.json").toURI()));
//            assertEquals("OpenAPIFileHandler", loaderSwaggerK8sOpenAPIV2.getModelFileHandler().toString());
//            loaderAWSMediaConnectSwagger.loadModelFromFile(new File(getClass().getResource("AWS-MediaConnect-swagger.json").toURI()));
//            assertEquals("OpenAPIFileHandler", loaderAWSMediaConnectSwagger.getModelFileHandler().toString());
            loaderDigitalOceanSwagger.loadModelFromFile(new File(getClass().getResource("DigitalOcean-public.v2.yaml").toURI()));
            assertEquals("OpenAPIFileHandler", loaderDigitalOceanSwagger.getModelFileHandler().toString());
            loaderAdyen.loadModelFromFile(new File(getClass().getResource("Adyen-Checkout API-swagger.yaml").toURI()));
            assertEquals("OpenAPIFileHandler", loaderAdyen.getModelFileHandler().toString());
        } catch (Exception e) {
            fail(e);
        }

        CodegenOperation op = OpenAPIUtils.getCodegenOperation(
            "/v2/1-clicks",
            "GET",
            getModel(loaderDigitalOceanSwagger));
        CodegenOperation op2 = OpenAPIUtils.getCodegenOperation(
            "/v2/1-clicks",
            "GET",
            getUnresolvedModel(loaderDigitalOceanSwagger));
        CodegenOperation op3 = OpenAPIUtils.getCodegenOperation(
            "/v2/account/keys",
            "POST",
            getModel(loaderDigitalOceanSwagger));
        CodegenOperation op4 = OpenAPIUtils.getCodegenOperation(
            "/v2/account/keys",
            "POST",
            getUnresolvedModel(loaderDigitalOceanSwagger));
        CodegenOperation op5 = OpenAPIUtils.getCodegenOperation(
            "/orders/cancel",
            "POST",
            getModel(loaderAdyen));
        List<SecurityRequirement> sec = getModel(loaderDigitalOceanSwagger).getSecurity();
        Map<String, SecurityScheme> sec2 = getModel(loaderDigitalOceanSwagger).getComponents().getSecuritySchemes();
        List<SecurityRequirement> sec3 = getModel(loaderAdyen).getSecurity();
        Map<String, SecurityScheme> sec4 = getModel(loaderAdyen).getComponents().getSecuritySchemes();
        System.out.println(op.hasAuthMethods);
        System.out.println(op.authMethods);

        System.out.println(getModel(loaderDigitalOceanSwagger)
            .getPaths()
            .get("/v2/1-clicks")
            .readOperationsMap()
            .get(PathItem.HttpMethod.valueOf("GET"))
            .getSecurity());

//        System.out.println(getModel(loaderDigitalOceanSwagger)
//            .getPaths()
//            .get("/v2/apps/{app_id}/metrics/bandwidth_daily")
//            .getGet());

//        ResolverCache rc = new ResolverCache(getModel(loaderDigitalOceanSwagger), null, null);
//        System.out.println(
//            rc.loadRef("#/paths/~1v2~1account~1keys/get/responses/200/content/application~1json/schema/allOf/0/properties/ssh_keys/items",
//                RefFormat.INTERNAL, Object.class));
//        System.out.println(
//            rc.loadRef("#/paths//v2/account/keys/get/responses/200/content/application/json/schema/allOf/0/properties/ssh_keys/items",
//            RefFormat.INTERNAL, Object.class));
//        System.out.println(
//            rc.loadRef("#/paths",
//            RefFormat.INTERNAL, Object.class));

//        OpenAPIExamplesExtractor extractor =
//            new OpenAPIExamplesExtractor(
//                getModel(loaderSampleOpenAPIExhaustive),
//                getUnresolvedModel(loaderSampleOpenAPIExhaustive)
//            );
//
//        // Example 1: Get all examples for application/json
//        Map<String, String> jsonExamples = extractor.getRequestBodyExamples(
//            "/users",
//            "post",
//            "application/json"
//        );
//
//        System.out.println("All JSON examples:");
//        jsonExamples.forEach((name, value) -> {
//            System.out.println(name + ": " + value);
//        });
//
//        // Example 2: Get a specific named example
//        String adminExample = extractor.getRequestBodyExampleByName(
//            "/users",
//            "post",
//            "application/json",
//            "adminUser"
//        );
//        System.out.println("\nAdmin user example: " + adminExample);
//
//        // Example 3: Get random example
//        String randomExample = extractor.getRandomRequestBodyExample(
//            "/users",
//            "post",
//            "application/json"
//        );
//        System.out.println("\nRandom example: " + randomExample);
//
//        // Example 4: Print all examples across all content types
//        extractor.printAllExamples("/users", "post");
//
//        // Example 5: Generate body content (for your use case)
//        String bodyContent = extractor.generateBodyContent(
//            "/users",
//            "post",
//            "application/json",
//            false  // false = first example, true = random
//        );
//        System.out.println("\nGenerated body content: " + bodyContent);
//
//        Map<String, String> paramExamples = extractor.getParameterExamples(
//            "/users", "get", "filter", "query");
//        System.out.println("\nParameter examples for GET /users filter:");
//        paramExamples.forEach((name, value) -> {
//            System.out.println(name + ": " + value);
//        });
//
//        String randomParamExample = extractor.getRandomParameterExample(
//            "/users", "get", "filter", "query");
//        System.out.println("\nRandom parameter example for GET /users filter: " + randomParamExample);
//
//        bodyContent = extractor.generateBodyContent(
//            "/users",
//            "post",
//            "application/xml",
//            true  // false = first example, true = random
//        );
//        System.out.println("\nGenerated body content: " + bodyContent);
//
//        randomExample = extractor.getRandomRequestBodyExample(
//            "/users",
//            "post",
//            "application/xml"
//        );
//        System.out.println("Random example: " + randomExample);
//        System.out.println(OpenAPIExampleGenerator.generate(
//            getModel(loaderSampleOpenAPIExhaustive),
//            getUnresolvedModel(loaderSampleOpenAPIExhaustive),
//            "POST",
//            "/users",
//            "application/json"
//        ));
//
//        String deleteExample = OpenAPIExampleGenerator.generate(
//            getModel(loaderSwaggerK8sOpenAPIV2),
//            getUnresolvedModel(loaderSwaggerK8sOpenAPIV2),
//            "DELETE",
//            "/api/v1/namespaces/{namespace}/configmaps",
//            "*/*"
//        );
//        System.out.println(deleteExample);
//        System.out.println("1 is null: %s".formatted(getModel(loaderSwaggerK8sOpenAPIV2).getPaths().get("/api/v1/namespaces/{namespace}/configmaps").getDelete().getRequestBody().getContent() == null));
//        System.out.println("1 keyset: %s".formatted(getModel(loaderSwaggerK8sOpenAPIV2).getPaths().get("/api/v1/namespaces/{namespace}/configmaps").getDelete().getRequestBody().getContent().keySet()));
//        System.out.println("2 is null: %s".formatted(getUnresolvedModel(loaderSwaggerK8sOpenAPIV2).getPaths().get("/api/v1/namespaces/{namespace}/configmaps").getDelete().getRequestBody().getContent() == null));
//        System.out.println("3 is null: %s".formatted(getModel(loaderSwaggerK8sOpenAPIV2).getPaths().get("/api/v1/namespaces/{namespace}/configmaps").readOperationsMap().get(PathItem.HttpMethod.DELETE).getRequestBody().getContent() == null));
//        System.out.println("3 keyset: %s".formatted(getModel(loaderSwaggerK8sOpenAPIV2).getPaths().get("/api/v1/namespaces/{namespace}/configmaps").readOperationsMap().get(PathItem.HttpMethod.DELETE).getRequestBody().getContent().keySet()));
//        System.out.println("4 is null: %s".formatted(getUnresolvedModel(loaderSwaggerK8sOpenAPIV2).getPaths().get("/api/v1/namespaces/{namespace}/configmaps").readOperationsMap().get(PathItem.HttpMethod.DELETE).getRequestBody().getContent() == null));

//        System.out.println(getModel(loaderAWSMediaConnectSwagger)
//            .getPaths()
//            .get("/v1/bridges/{bridgeArn}/outputs")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.POST)
//            .getRequestBody()
//            .getContent()
//            .get("application/json")
//            .getSchema()
//        );
//        System.out.println(
//            ((Schema<?>) getModel(loaderAWSMediaConnectSwagger)
//            .getPaths()
//            .get("/v1/bridges/{bridgeArn}/outputs")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.POST)
//            .getRequestBody()
//            .getContent()
//            .get("application/json")
//            .getSchema()
//            .getProperties()
//            .get("outputs"))
//            .getItems()
//        );
//        System.out.println(getModel(loaderAWSMediaConnectSwagger)
//            .getPaths()
//            .get("/v1/bridges/{bridgeArn}/outputs")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.POST)
//            .getRequestBody()
//            .getContent()
//            .get("application/json")
//            .getSchema()
//            .getProperties()
//            .get("outputs") instanceof Schema);
//        System.out.println(getModel(loaderAWSMediaConnectSwagger)
//            .getPaths()
//            .get("/v1/bridges/{bridgeArn}/outputs")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.POST)
//            .getRequestBody()
//            .getContent()
//            .get("application/json")
//            .getSchema()
//            .getProperties()
//            .get("outputs") instanceof ArraySchema);
//        System.out.println(new OpenAPIExampleGenerator.ExampleGenerator(
//            getModel(loaderAWSMediaConnectSwagger).getComponents().getSchemas(),
//            getUnresolvedModel(loaderAWSMediaConnectSwagger)
//        ).generate(
//            null,
//            new ArrayList<>(List.of("application/json")), // ExampleGenerator works best with JSON, then we convert later if needed
//            getModel(loaderAWSMediaConnectSwagger)
//                .getPaths()
//                .get("/v1/bridges/{bridgeArn}/outputs")
//                .readOperationsMap()
//                .get(PathItem.HttpMethod.POST)
//                .getRequestBody()
//                .getContent()
//                .get("application/json")
//                .getSchema()
//        ));
//        System.out.println(OpenAPIExampleGenerator.generate(
//            getModel(loaderAWSMediaConnectSwagger),
//            getUnresolvedModel(loaderAWSMediaConnectSwagger),
//            "POST",
//            "/v1/bridges/{bridgeArn}/outputs",
//            "application/json"
//        ));
//        System.out.println(OpenAPIExampleGenerator.generate(
//            getModel(loaderSwaggerK8sOpenAPIV2),
//            getUnresolvedModel(loaderSwaggerK8sOpenAPIV2),
//            "PATCH",
//            "/api/v1/namespaces/{namespace}/configmaps/{name}",
//            "application/apply-patch+yaml"
//        ));
//        System.out.println(getUnresolvedModel(loaderSwaggerK8sOpenAPIV2)
//            .getPaths()
//            .get("/api/v1/namespaces/{namespace}/configmaps/{name}")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.valueOf("PATCH"))
//            .getRequestBody()
//            .getContent()
//        );
//        System.out.println(getUnresolvedModel(loaderSwaggerK8sOpenAPIV2)
//            .getPaths()
//            .get("/api/v1/namespaces/{namespace}/configmaps/{name}")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.valueOf("PATCH"))
//            .getRequestBody()
//        );
//        System.out.println(getUnresolvedModel(loaderSwaggerK8sOpenAPIV2)
//            .getPaths()
//            .get("/api/v1/namespaces/{namespace}/configmaps/{name}")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.valueOf("PATCH"))
//        );
//        System.out.println(getModel(loaderSwaggerK8sOpenAPIV2)
//            .getPaths()
//            .get("/api/v1/namespaces/{namespace}/configmaps/{name}")
//            .readOperationsMap()
//            .get(PathItem.HttpMethod.valueOf("PATCH"))
//            .getRequestBody()
//            .getContent()
//            .get("application/apply-patch+yaml")
//            .getSchema()
//        );
    }
}
