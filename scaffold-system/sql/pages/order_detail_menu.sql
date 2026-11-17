-- List的菜单与权限点
-- 由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。权限点由模型编码派生
-- （order_detail:order_detail:*），与生成的 Controller 上的 @RequiresPermission 一一对应 ——
-- 只改一边就会出现「页面在、点进去 403」。
--
-- 菜单 ID 交给数据库自增分配（底座原来按「租户各自 MAX+1」分配，两个租户会算出同一个 ID 撞主键，
-- V4 已改成自增），所以这里不能沿用底座的「固定 ID + INSERT IGNORE」写法，改成按 permission 判重：
-- 本文件重复执行不会产生第二条菜单，也不用猜本项目已经用到哪个 ID。
--
-- 重发布会把菜单行更新到最新口径：只 INSERT 不 UPDATE 的话，页面改了路由、图标、
-- 排序或归组之后，目标库里那条旧菜单永远不会跟着走。
--
-- 本页归在路由分组「交易」下：先确保分组目录存在（判重口径是「顶层 + dir + 路径」），
-- 再把页面菜单挂到它下面。最终路径 = 分组前缀 /trade + 页面路径 = /trade/orders。
-- 给内置管理员（role_id = 1）之外的角色授权页面时，要把这条目录一起勾上：
-- 菜单树按角色授权取行，只有子页面没有父目录时，那个页面会被当成顶层项显示。
-- 图标可选值见物料包 data/icons.ts。component 就是前端路由解析的相对路径：
-- order_detail/OrderDetail → src/views/order_detail/OrderDetail.vue，该文件同样由平台生成并在同一次发布里推上去。

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT 0, '交易', 'dir', 'gear', '/trade', '', '', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND menu_type = 'dir' AND path = '/trade');

UPDATE sys_menu
SET menu_name = '交易',
    icon = 'gear',
    sort = 1,
    update_by = 'system'
WHERE parent_id = 0 AND menu_type = 'dir' AND path = '/trade';

SET @order_detail_group := (SELECT id FROM sys_menu WHERE parent_id = 0 AND menu_type = 'dir' AND path = '/trade' LIMIT 1);

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @order_detail_group, 'List', 'menu', 'cart', '/trade/orders', 'order_detail/OrderDetail', 'order_detail:order_detail:list', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'order_detail:order_detail:list');

SET @order_detail_menu := (SELECT id FROM sys_menu WHERE permission = 'order_detail:order_detail:list' LIMIT 1);

UPDATE sys_menu
SET parent_id = @order_detail_group,
    menu_name = 'List',
    menu_type = 'menu',
    icon = 'cart',
    path = '/trade/orders',
    component = 'order_detail/OrderDetail',
    sort = 1,
    update_by = 'system'
WHERE permission = 'order_detail:order_detail:list';

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @order_detail_menu, '新增订单明细', 'btn', '', '', '', 'order_detail:order_detail:add', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'order_detail:order_detail:add');

UPDATE sys_menu
SET parent_id = @order_detail_menu,
    menu_name = '新增订单明细',
    sort = 1,
    update_by = 'system'
WHERE permission = 'order_detail:order_detail:add';

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @order_detail_menu, '编辑订单明细', 'btn', '', '', '', 'order_detail:order_detail:edit', 2, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'order_detail:order_detail:edit');

UPDATE sys_menu
SET parent_id = @order_detail_menu,
    menu_name = '编辑订单明细',
    sort = 2,
    update_by = 'system'
WHERE permission = 'order_detail:order_detail:edit';

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @order_detail_menu, '删除订单明细', 'btn', '', '', '', 'order_detail:order_detail:remove', 3, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'order_detail:order_detail:remove');

UPDATE sys_menu
SET parent_id = @order_detail_menu,
    menu_name = '删除订单明细',
    sort = 3,
    update_by = 'system'
WHERE permission = 'order_detail:order_detail:remove';

-- 授给内置管理员角色（role_id = 1，与 V2 种子里的角色同一个），同样按已存在判重。
-- 其它角色要不要看到这个页面，请在项目的角色管理里勾，不由平台代做。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.permission IN ('order_detail:order_detail:list', 'order_detail:order_detail:add', 'order_detail:order_detail:edit', 'order_detail:order_detail:remove')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
