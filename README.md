# KuangBianXiaoHaiLuo / 狂扁小海螺

![built-with-love](https://img.shields.io/badge/built%20with-%E2%9D%A4-red.svg) ![github-last-commit](https://img.shields.io/github/last-commit/ustc-zzzz/KuangBianXiaoHaiLuo.svg?color=yellow) ![github-license](https://img.shields.io/github/license/ustc-zzzz/KuangBianXiaoHaiLuo.svg) ![authorized-by-izzel-aliz](https://img.shields.io/badge/officially%20authorized%20by-IzzelAliz-blue.svg)

The Construction Site of Mini Game Plugin KuangBianXiaoHaiLuo / 狂扁小海螺小游戏插件的施工现场

## 策划

* 玩家输入 `/kbxhl start` 命令开始小游戏
  * 玩家的背包将会清空，手中固定手持一把石斧
  * 玩家四周各 3x3 共计 36 格区域将不定期刷新显示名称为“海螺”的潜影贝
  * 潜影贝的显示时间极短，玩家必须在潜影贝消失前击中潜影贝，否则不能得分
  * 玩家对潜影贝使用左键潜影贝消失，同时玩家获得对应的分数
  * 共获得 651 个潜影壳结束游戏，统计整个游戏的耗费时间
* 玩家输入 `/kbxhl stop` 强制结束游戏
  * 游戏结束时背包归位
  * 游戏进行中背包内物品不得移动
* 玩家输入 `/kbxhl top` 显示排行榜
  * 所有正常结束游戏的玩家都将会将成绩自动上传排行榜
  * 排行榜按耗费时间从小到大正序排列

## 数值

* 普通海螺（N）：获得 1 分，属于出现概率最高的海螺
* 稀有海螺（R）：获得 5 分，出现概率是普通海螺的二分之一
* 超级稀有海螺（SR）：获得 25 分，出现概率是稀有海螺的三分之一
* 特级稀有海螺（SSR）：获得 125 分，出现概率是超级稀有海螺的二分之一

## 命令

* `/kbxhl`
  * `/kbxhl start`
  * `/kbxhl stop`
  * `/kbxhl top`

## 权限

* `kbxhl.command`：所有相关权限
  * `kbxhl.command.start`：开始游戏的权限
  * `kbxhl.command.stop`：结束游戏的权限
  * `kbxhl.command.top`：显示排行榜的权限
