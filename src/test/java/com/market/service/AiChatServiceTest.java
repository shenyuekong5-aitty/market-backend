package com.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiChatServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void knowledgeMatchesBusinessQuestions() throws Exception {
        AiKnowledgeService knowledge = new AiKnowledgeService(mapper);
        assertEquals("apply", knowledge.search("我想申请摊位怎么办？", 1).get(0).id());
        assertEquals("change", knowledge.search("我想更换摊位", 1).get(0).id());
        assertEquals("payment", knowledge.search("支付订单怎么操作？", 1).get(0).id());
        assertEquals("browse-goods", knowledge.search("有那些商品", 1).get(0).id());
        assertTrue(knowledge.search("今天天气怎么样", 1).isEmpty());
        assertFalse(knowledge.suggestions().isEmpty());
        assertTrue(AiCatalogService.isCatalogQuestion("有那些商品"));
        assertTrue(AiCatalogService.isCatalogQuestion("商品"));
        assertFalse(AiCatalogService.isCatalogQuestion("如何上架商品"));
    }

    @Test
    void noKeyFallsBackAndSessionsBelongToOneUser() throws Exception {
        AiChatService service = new AiChatService(new AiKnowledgeService(mapper), emptyCatalog(), mapper);
        var first = service.chat(1L, "如何申请摊位？", null);
        assertEquals("knowledge", first.source());
        assertEquals("not_configured", first.providerStatus());
        assertTrue(first.answer().contains("管理员审批"));
        assertEquals(first.sessionId(), service.chat(1L, "如何预订商品？", first.sessionId()).sessionId());
        assertNotEquals(first.sessionId(), service.chat(2L, "如何申请摊位？", first.sessionId()).sessionId());
        assertEquals("unknown", service.chat(1L, "今天天气怎么样", null).source());
    }

    @Test
    void catalogQuestionUsesDatabaseAndEmptyCatalogIsHonest() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AiCatalogService catalog = new AiCatalogService(jdbc);
        assertTrue(catalog.answerIfCatalogQuestion("如何上架商品").isEmpty());
        verifyNoInteractions(jdbc);
        doReturn(java.util.List.of()).when(jdbc).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any());
        assertTrue(catalog.answerIfCatalogQuestion("有那些商品").orElseThrow().contains("没有查询到"));
    }

    @Test
    void providerFailureFallsBackToKnowledge() throws Exception {
        AiChatService service = new AiChatService(new AiKnowledgeService(mapper), emptyCatalog(), mapper);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        var answer = service.chat(1L, "怎么取消订单？", null);
        assertEquals("knowledge", answer.source());
        assertEquals("unavailable", answer.providerStatus());
        assertTrue(answer.answer().contains("我的订单"));
    }

    @Test
    void configuredProviderUsesChatCompletionsAndReturnsModelAnswer() throws Exception {
        AiChatService service = new AiChatService(new AiKnowledgeService(mapper), emptyCatalog(), mapper);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://token.sensenova.cn/v1");
        ReflectionTestUtils.setField(service, "model", "sensenova-6.8-flash-lite");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(service, "http");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(http).build();
        mockServer.expect(requestTo("https://token.sensenova.cn/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"你好，我是集市客服。\"}}]}", MediaType.APPLICATION_JSON));

        var response = service.chat(1L, "你好", null);
        assertEquals("ai", response.source());
        assertEquals("online", response.providerStatus());
        assertEquals("你好，我是集市客服。", response.answer());
        mockServer.verify();
    }

    private AiCatalogService emptyCatalog() {
        AiCatalogService catalog = mock(AiCatalogService.class);
        when(catalog.answerIfCatalogQuestion(anyString())).thenReturn(Optional.empty());
        return catalog;
    }
}
