package com.scaffold.business.sales_order.mapper;

import com.scaffold.business.sales_order.domain.SalesOrder;
import com.scaffold.business.sales_order.domain.SalesOrderRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售订单主表（表 {@code sales_order}）的数据访问。
 *
 * <p>本类由元搭低代码平台按模型定义生成，属 B 档：每次发布整文件覆盖，要自定义查询请回到设计器
 * 改字段与查询方式，或在同包的 {@code SalesOrderExtension}（C 档）里另写。
 *
 * <p>查询条件是「参数为空就是不限制」的<b>静态 SQL</b>，不用动态标签拼 WHERE：任何参数组合下
 * 都是同一条语句，贴着读就知道会查什么；运算符只来自 QueryType 枚举，前端传来的值进不了 SQL 结构。
 * 表名与列名不带反引号 —— MySQL 保留字在建模时就被拦住了（见 {@code SqlIdentifiers}）。
 */
@Mapper
public interface SalesOrderMapper {

    /** 列表总数。WHERE 段与 {@link #selectRows} 共用一份，两处口径不会分叉 */
    @Select("""
            SELECT COUNT(*)
            FROM sales_order t
            WHERE t.deleted = 0
              AND (#{orderNo} IS NULL OR #{orderNo} = ''
                   OR t.order_no LIKE CONCAT('%', #{orderNo}, '%'))
              AND (#{amountStart} IS NULL OR t.amount >= #{amountStart})
              AND (#{amountEnd} IS NULL OR t.amount <= #{amountEnd})
              AND (#{status} IS NULL OR t.status = #{status})
            """)
    long countByQuery(
            @Param("orderNo") String orderNo,
            @Param("amountStart") BigDecimal amountStart,
            @Param("amountEnd") BigDecimal amountEnd,
            @Param("status") Integer status
    );

    /** 一页列表行。排序固定按主键倒序（最新在前）：不按字段名猜「哪个才是排序列」 */
    @Select("""
            SELECT t.id,
                   t.order_no AS orderNo,
                   t.amount,
                   t.status,
                   t.delivery_date AS deliveryDate,
                   t.remark
            FROM sales_order t
            WHERE t.deleted = 0
              AND (#{orderNo} IS NULL OR #{orderNo} = ''
                   OR t.order_no LIKE CONCAT('%', #{orderNo}, '%'))
              AND (#{amountStart} IS NULL OR t.amount >= #{amountStart})
              AND (#{amountEnd} IS NULL OR t.amount <= #{amountEnd})
              AND (#{status} IS NULL OR t.status = #{status})
            ORDER BY t.id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<SalesOrderRow> selectRows(
            @Param("orderNo") String orderNo,
            @Param("amountStart") BigDecimal amountStart,
            @Param("amountEnd") BigDecimal amountEnd,
            @Param("status") Integer status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 按主键取整行（含没勾选进列表的字段）。
     * <p>编辑弹窗必须先取这一份：列表行只有勾选进列表的列，直接拿它填表单，
     * 会把没显示出来的字段当成空覆盖掉。
     */
    @Select("""
            SELECT t.id,
                   t.order_no AS orderNo,
                   t.amount,
                   t.status,
                   t.delivery_date AS deliveryDate,
                   t.remark,
                   t.create_by AS createBy,
                   t.update_by AS updateBy,
                   t.create_time AS createTime,
                   t.update_time AS updateTime
            FROM sales_order t
            WHERE t.id = #{id} AND t.deleted = 0
            """)
    SalesOrder selectById(@Param("id") Long id);

    /** 订单编号是否已被占用：新增时 {@code excludeId} 传 null，修改时传自身主键 */
    @Select("""
            SELECT COUNT(*)
            FROM sales_order
            WHERE order_no = #{value}
              AND deleted = 0
              AND (#{excludeId} IS NULL OR id <> #{excludeId})
            """)
    long countByOrderNo(@Param("value") String value, @Param("excludeId") Long excludeId);

    /**
     * 新增销售订单主表，自增主键回写进实体的 {@code id}。
     * <p>创建 / 更新时间不写进语句，交给表上的 {@code DEFAULT CURRENT_TIMESTAMP} 与
     * {@code ON UPDATE CURRENT_TIMESTAMP} —— 与底座同一口径，也让数据库成为时间的唯一出处。
     */
    @Insert("""
            INSERT INTO sales_order (order_no, amount, status, delivery_date, remark, create_by, update_by)
            VALUES (#{salesOrder.orderNo}, #{salesOrder.amount}, #{salesOrder.status}, #{salesOrder.deliveryDate}, #{salesOrder.remark}, #{operator}, #{operator})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "salesOrder.id")
    int insert(
            @Param("salesOrder") SalesOrder salesOrder,
            @Param("operator") String operator
    );

    /**
     * 按主键覆盖勾选了「可编辑」的字段（表单提交的是整个表单，没勾选的字段一个都不碰）。
     * <p>返回受影响行数，0 就是这一行不存在或已删除，由 Service 换算成业务提示。
     */
    @Update("""
            UPDATE sales_order
            SET order_no = #{salesOrder.orderNo},
                amount = #{salesOrder.amount},
                status = #{salesOrder.status},
                delivery_date = #{salesOrder.deliveryDate},
                remark = #{salesOrder.remark},
                update_by = #{operator}
            WHERE id = #{id} AND deleted = 0
            """)
    int update(
            @Param("id") Long id,
            @Param("salesOrder") SalesOrder salesOrder,
            @Param("operator") String operator
    );

    /** 软删：只翻删除标记，历史留在库里 —— 这是模型上勾了「逻辑删除」之后的口径 */
    @Update("""
            UPDATE sales_order
            SET deleted = 1, update_by = #{operator}
            WHERE id = #{id} AND deleted = 0
            """)
    int deleteById(
            @Param("id") Long id,
            @Param("operator") String operator
    );
}
