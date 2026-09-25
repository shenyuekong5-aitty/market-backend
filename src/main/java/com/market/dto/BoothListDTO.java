package com.market.dto;

import com.market.entity.Booth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoothListDTO {
    /** 摊位总数 */
    private long total;
    /** 摊位列表 */
    private List<Booth> list;
}
