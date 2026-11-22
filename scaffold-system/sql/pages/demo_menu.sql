-- 测试的菜单与权限点
-- 由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。权限点由模型编码派生
-- （demo:demo:*），与生成的 Controller 上的 @RequiresPermission 一一对应 ——
-- 只改一边就会出现「页面在、点进去 403」。
--
-- 菜单 ID 交给数据库自增分配（底座原来按「租户各自 MAX+1」分配，两个租户会算出同一个 ID 撞主键，
-- V4 已改成自增），所以这里不能沿用底座的「固定 ID + INSERT IGNORE」写法，改成按 permission 判重：
-- 本文件重复执行不会产生第二条菜单，也不用猜本项目已经用到哪个 ID。
--
-- 重发布会把菜单行更新到最新口径：只 INSERT 不 UPDATE 的话，页面改了路由、图标、
-- 排序或归组之后，目标库里那条旧菜单永远不会跟着走。
--
-- 本页归在路由分组「订单」下：先确保分组目录存在（判重口径是「顶层 + dir + 路径」），
-- 再把页面菜单挂到它下面。最终路径 = 分组前缀 /order + 页面路径 = /order/demo。
-- 给内置管理员（role_id = 1）之外的角色授权页面时，要把这条目录一起勾上：
-- 菜单树按角色授权取行，只有子页面没有父目录时，那个页面会被当成顶层项显示。
-- 图标可选值见物料包 data/icons.ts。component 就是前端路由解析的相对路径：
-- demo/Demo → src/views/demo/Demo.vue，该文件同样由平台生成并在同一次发布里推上去。

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT 0, '订单', 'dir', 'grid', '/order', '', '', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND menu_type = 'dir' AND path = '/order');

UPDATE sys_menu
SET menu_name = '订单',
    icon = 'grid',
    sort = 1,
    visible = 1,
    status = 1,
    update_by = 'system'
WHERE parent_id = 0 AND menu_type = 'dir' AND path = '/order';

SET @demo_group := (SELECT id FROM sys_menu WHERE parent_id = 0 AND menu_type = 'dir' AND path = '/order' LIMIT 1);

-- 先记住旧父目录，再移动页面菜单。路由改组或删掉分组后，旧目录只有在没有其它页面时才隐藏，
-- 不能按路径直接处理，否则同一目录下仍在使用的页面会被一起撤掉。
SET @demo_previous_group := (SELECT parent_id FROM sys_menu WHERE permission = 'demo:demo:list' LIMIT 1);

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @demo_group, '测试', 'menu', 'shield', '/order/demo', 'demo/Demo', 'demo:demo:list', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'demo:demo:list');

SET @demo_menu := (SELECT id FROM sys_menu WHERE permission = 'demo:demo:list' LIMIT 1);

UPDATE sys_menu
SET parent_id = @demo_group,
    menu_name = '测试',
    menu_type = 'menu',
    icon = 'shield',
    path = '/order/demo',
    component = 'demo/Demo',
    sort = 1,
    update_by = 'system'
WHERE permission = 'demo:demo:list';

UPDATE sys_menu previous_group
LEFT JOIN sys_menu child ON child.parent_id = previous_group.id
SET previous_group.visible = 0,
    previous_group.status = 0,
    previous_group.update_by = 'system'
WHERE previous_group.id = @demo_previous_group
  AND previous_group.id <> @demo_group
  AND previous_group.menu_type = 'dir'
  AND child.id IS NULL;

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @demo_menu, '新增测试模型', 'btn', '', '', '', 'demo:demo:add', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'demo:demo:add');

UPDATE sys_menu
SET parent_id = @demo_menu,
    menu_name = '新增测试模型',
    sort = 1,
    update_by = 'system'
WHERE permission = 'demo:demo:add';

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @demo_menu, '编辑测试模型', 'btn', '', '', '', 'demo:demo:edit', 2, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'demo:demo:edit');

UPDATE sys_menu
SET parent_id = @demo_menu,
    menu_name = '编辑测试模型',
    sort = 2,
    update_by = 'system'
WHERE permission = 'demo:demo:edit';

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @demo_menu, '删除测试模型', 'btn', '', '', '', 'demo:demo:remove', 3, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'demo:demo:remove');

UPDATE sys_menu
SET parent_id = @demo_menu,
    menu_name = '删除测试模型',
    sort = 3,
    update_by = 'system'
WHERE permission = 'demo:demo:remove';

-- 授给内置管理员角色（role_id = 1，与 V2 种子里的角色同一个），同样按已存在判重。
-- 其它角色要不要看到这个页面，请在项目的角色管理里勾，不由平台代做。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.permission IN ('demo:demo:list', 'demo:demo:add', 'demo:demo:edit', 'demo:demo:remove')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
