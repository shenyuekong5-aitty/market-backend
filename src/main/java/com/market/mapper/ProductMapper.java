package com.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.market.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT id FROM product WHERE id = #{productId} FOR UPDATE")
    Long lockId(@Param("productId") Long productId);

    @Update("UPDATE product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity} AND sale_status = '上架'")
    int decrementStockIfAvailable(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int incrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
