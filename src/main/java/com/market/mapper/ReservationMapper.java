package com.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
    @Update("UPDATE reservation SET status = #{nextStatus} WHERE id = #{id} AND status = '待确认'")
    int transitionIfPending(@Param("id") Long id, @Param("nextStatus") String nextStatus);
}
