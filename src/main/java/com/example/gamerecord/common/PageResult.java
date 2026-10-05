package com.example.gamerecord.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;
    /** 总记录数 */
    private Long total;
    /** 当前页码（从 1 开始） */
    private Long page;
    /** 每页条数 */
    private Long size;

    public static <T> PageResult<T> of(Page<?> page, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }
}
