## 什么么开发这个组件

希望以树形结构数据为核心，连接万事万物，达到一个网站完成所有事情的目的。

## Tree是什么

几个基本概念

- `I` 唯一键
- `E extends Element<I>` 挂载在节点上的元素
- `N extends Node<I, E, N>` 节点

## Tree的使用

### 1、创建元素类

```java

@Data
static class Ele implements Element<String> {
    private final String id;
    private final String parentId;
}
```

### 2、使用Node创建Tree，并添加元素

- `DeepNode` 默认Node类型

```java
public static void main(String[] args) {
    List<Ele> es = new ArrayList<>();
    es.add(new Ele("1", "p1"));
    es.add(new Ele("2", "p1"));
    es.add(new Ele("3", "p1"));
    es.add(new Ele("p1", null));
    es.add(new Ele("p2", null));
    Tree<String, Ele, DefaultNode<String, Ele>> defaultNodeTree = Tree.of(new DefaultNode<>());
    defaultNodeTree.add(es);
    System.out.println(Jsons.str(defaultNodeTree));
}
```

### 3、查看数据结构

  ```json
  {
  "root": {
    "children": [
      {
        "element": {
          "id": "p1"
        },
        "children": [
          {
            "element": {
              "id": "1",
              "parentId": "p1"
            }
          }, {
            "element": {
              "id": "2",
              "parentId": "p1"
            }
          }
        ]
      }, {
        "element": {
          "id": "p2"
        },
        "children": [
          {
            "element": {
              "id": "4",
              "parentId": "p2"
            }
          }
        ]
      }
    ]
  }
}
  ```

- `DeepNode` 带有深度标识的Node类型

```java
Tree<String, Ele, DeepNode<String, Ele>> deepNodeTree = Tree.of(new DeepNode<>(true));
```

```json
 {
  "root": {
    "children": [
      {
        "element": {
          "id": "p1"
        },
        "children": [
          {
            "element": {
              "id": "1",
              "parentId": "p1"
            },
            "depth": 2
          }, {
            "element": {
              "id": "2",
              "parentId": "p1"
            },
            "depth": 2
          }, {
            "element": {
              "id": "3",
              "parentId": "p1"
            },
            "depth": 2
          }
        ],
        "depth": 1
      }, {
        "element": {
          "id": "p2"
        },
        "depth": 1
      }
    ],
    "depth": 0
  }
}
```

- `FlatNode` 展开Element的Node

```java
Tree<String, Ele, FlatNode<String, Ele>> flatNodeTree = Tree.of(new FlatNode<>(List.of(Ele::getId, Ele::getParentId)));
```

```json
{
  "root": {
    "children": [
      {
        "children": [
          {
            "id": "1",
            "parentId": "p1"
          }, {
            "id": "2",
            "parentId": "p1"
          }, {
            "id": "3",
            "parentId": "p1"
          }
        ],
        "id": "p1"
      }, {
        "id": "p2"
      }
    ]
  }
}
```

### `EnhanceNode` 增强节点类型

支持children排序  
支持多个父节点，交叉关联

- 创建元素类

```java

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
static class Ele2 extends EnhanceElement<String> {
    private final String id;
    private final String parentId;
    private final String sorted;

    @Override
    public int compareTo(@NotNull EnhanceElement<String> o) {
        return Element.comparator(this, (Ele2) o, Ele2::getSorted);
    }
}
```

- `DefaultEnhanceNode` 默认增强节点类

```java
EnhanceTree<String, Ele2, DefaultEnhanceNode<String, Ele2>> enhanceTree = EnhanceTree.of(new DefaultEnhanceNode<String, Ele2>());
```

```json
{
  "root": {
    "children": [
      {
        "element": {
          "id": "p1",
          "sorted": "2"
        },
        "children": [
          {
            "element": {
              "id": "3",
              "parentId": "p1",
              "sorted": "0"
            }
          }, {
            "element": {
              "id": "1",
              "parentId": "p1",
              "sorted": "1"
            },
            "parents": [
              {
                "id": "p1",
                "sorted": "2"
              }, {
                "id": "p2",
                "sorted": "2"
              }
            ]
          }, {
            "element": {
              "id": "2",
              "parentId": "p1",
              "sorted": "2"
            }
          }
        ]
      }, {
        "element": {
          "id": "p2",
          "sorted": "2"
        },
        "children": [
          {
            "element": {
              "id": "1",
              "parentId": "p1",
              "sorted": "1"
            },
            "parents": [
              {
                "id": "p1",
                "sorted": "2"
              }, {
                "id": "p2",
                "sorted": "2"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

### 更多类型可以自行扩展


