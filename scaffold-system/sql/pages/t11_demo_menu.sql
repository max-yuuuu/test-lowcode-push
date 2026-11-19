-- T11 演示列表的菜单与权限点
-- 由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。权限点由模型编码派生
-- （t11_demo:t11_demo:*），与生成的 Controller 上的 @RequiresPermission 一一对应 ——
-- 只改一边就会出现「页面在、点进去 403」。
--
-- 菜单 ID 交给数据库自增分配（底座原来按「租户各自 MAX+1」分配，两个租户会算出同一个 ID 撞主键，
-- V4 已改成自增），所以这里不能沿用底座的「固定 ID + INSERT IGNORE」写法，改成按 permission 判重：
-- 本文件重复执行不会产生第二条菜单，也不用猜本项目已经用到哪个 ID。
--
-- 重发布会把菜单行更新到最新口径：只 INSERT 不 UPDATE 的话，页面改了路由、图标、
-- 排序或归组之后，目标库里那条旧菜单永远不会跟着走。
--
-- 本页没有路由分组：菜单挂在顶层（parent_id = 0）。
-- 图标可选值见物料包 data/icons.ts。component 就是前端路由解析的相对路径：
-- t11_demo/T11Demo → src/views/t11_demo/T11Demo.vue，该文件同样由平台生成并在同一次发布里推上去。

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT 0, 'T11 演示列表', 'menu', 'grid', '/t11_demo/t11_demo', 't11_demo/T11Demo', 't11_demo:t11_demo:list', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 't11_demo:t11_demo:list');

SET @t11_demo_menu := (SELECT id FROM sys_menu WHERE permission = 't11_demo:t11_demo:list' LIMIT 1);

UPDATE sys_menu
SET parent_id = 0,
    menu_name = 'T11 演示列表',
    menu_type = 'menu',
    icon = 'grid',
    path = '/t11_demo/t11_demo',
    component = 't11_demo/T11Demo',
    sort = 1,
    update_by = 'system'
WHERE permission = 't11_demo:t11_demo:list';

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status, create_by, update_by)
SELECT @t11_demo_menu, '新增T11 演示', 'btn', '', '', '', 't11_demo:t11_demo:add', 1, 1, 1, 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 't11_demo:t11_demo:add');

UPDATE sys_menu
SET parent_id = @t11_demo_menu,
    menu_name = '新增T11 演示',
    sort = 1,
    update_by = 'system'
WHERE permission = 't11_demo:t11_demo:add';

-- 授给内置管理员角色（role_id = 1，与 V2 种子里的角色同一个），同样按已存在判重。
-- 其它角色要不要看到这个页面，请在项目的角色管理里勾，不由平台代做。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.permission IN ('t11_demo:t11_demo:list', 't11_demo:t11_demo:add')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
