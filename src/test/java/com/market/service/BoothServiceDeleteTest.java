package com.market.service;

import com.market.entity.Booth;
import com.market.entity.Market;
import com.market.mapper.BoothMapper;
import com.market.mapper.MarketMapper;
import com.market.service.impl.BoothServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BoothServiceDeleteTest {
    private BoothServiceImpl service;
    private BoothMapper boothMapper;
    private MarketMapper marketMapper;

    @BeforeEach
    void setUp() {
        service = new BoothServiceImpl();
        boothMapper = mock(BoothMapper.class);
        marketMapper = mock(MarketMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", boothMapper);
        ReflectionTestUtils.setField(service, "marketMapper", marketMapper);

        Market market = new Market();
        market.setId(3L);
        market.setAdminId(7L);
        when(marketMapper.selectById(3L)).thenReturn(market);
    }

    private Booth booth(String status) {
        Booth booth = new Booth();
        booth.setId(11L);
        booth.setMarketId(3L);
        booth.setStatus(status);
        when(boothMapper.selectById(11L)).thenReturn(booth);
        return booth;
    }

    @Test
    void deletesStoppedBooth() {
        booth("停用");
        service.deleteBooth(11L, 7L);
        verify(boothMapper).deleteById(11L);
    }

    @Test
    void rejectsAvailableBoothUntilStopped() {
        booth("空闲");
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.deleteBooth(11L, 7L));
        assertEquals("请先停用摊位，再进行删除", error.getMessage());
        verify(boothMapper, never()).deleteById(11L);
    }

    @Test
    void rejectsOccupiedBooth() {
        booth("已占用");
        assertThrows(RuntimeException.class, () -> service.deleteBooth(11L, 7L));
        verify(boothMapper, never()).deleteById(11L);
    }
}
