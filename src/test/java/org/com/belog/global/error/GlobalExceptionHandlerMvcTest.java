package org.com.belog.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.Min;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindException;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerMvcTest.TestController.class})
@DisplayName("전역 예외 처리 테스트")
class GlobalExceptionHandlerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("경로 변수의 타입이 올바르지 않으면 400 응답을 반환한다")
    void invalidPathVariableTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/numbers/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CMN-E001"))
                .andExpect(jsonPath("$.data.fieldErrors[0].field").value("id"));
    }

    @Test
    @DisplayName("필수 요청 파라미터가 누락되면 400 응답을 반환한다")
    void missingRequestParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CMN-E001"));
    }

    @Test
    @DisplayName("메서드 파라미터 검증에 실패하면 400 응답을 반환한다")
    void methodValidationFailureReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/validated").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CMN-E001"))
                .andExpect(jsonPath("$.data.fieldErrors").isArray());
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type이면 415 응답을 반환한다")
    void unsupportedContentTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/test/json").contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("CMN-E007"));
    }

    @Test
    @DisplayName("지원하지 않는 응답 형식을 요청하면 406 응답을 반환한다")
    void unacceptableResponseTypeReturnsNotAcceptable() throws Exception {
        mockMvc.perform(get("/test/json").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    @DisplayName("바인딩에 실패하면 필드 오류와 함께 400 응답을 반환한다")
    void bindExceptionReturnsBadRequestWithFieldErrors() {
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.rejectValue("name", "required", "이름은 필수입니다.");

        var response = new GlobalExceptionHandler().handleBindException(new BindException(bindingResult));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CMN-E001");
        assertThat(response.getBody().data().fieldErrors().getFirst().field()).isEqualTo("name");
    }

    @Test
    @DisplayName("매핑되지 않은 경로를 요청하면 404 응답을 반환한다")
    void unmappedRequestReturnsNotFound() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CMN-E003"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/numbers/{id}")
        void number(@PathVariable(name = "id") Long id) {
        }

        @GetMapping("/required")
        void required(@RequestParam(name = "query") String query) {
        }

        @GetMapping("/validated")
        void validated(@RequestParam(name = "page") @Min(1) int page) {
        }

        @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        void consumeJson() {
        }

        @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
        void produceJson() {
        }
    }
}
