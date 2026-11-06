package com.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Update("UPDATE order_master SET status = '已付款', pay_time = #{payTime} WHERE id = #{id} AND status = '待付款'")
    int payIfPending(@Param("id") Long id, @Param("payTime") LocalDateTime payTime);

    @Update("UPDATE order_master SET status = '已取消' WHERE id = #{id} AND status = '待付款'")
    int cancelIfPending(@Param("id") Long id);

    @Update("UPDATE order_master SET status = '已取消' WHERE id = #{id} AND status = '待付款' AND create_time <= #{deadline}")
    int cancelIfExpired(@Param("id") Long id, @Param("deadline") LocalDateTime deadline);

    @Update("UPDATE order_master SET status = '待取货' WHERE id = #{id} AND status = '已付款'")
    int markReadyIfPaid(@Param("id") Long id);

    @Update("UPDATE order_master SET status = '已完成', complete_time = #{completeTime} WHERE id = #{id} AND status = '待取货'")
    int completeIfReady(@Param("id") Long id, @Param("completeTime") LocalDateTime completeTime);
}
