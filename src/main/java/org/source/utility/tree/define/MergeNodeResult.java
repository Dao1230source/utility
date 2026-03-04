package org.source.utility.tree.define;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MergeNodeResult<I extends Comparable<I>, E extends Element<I>, N extends AbstractNode<I, E, N>> {
    private N newNode;
    private @Nullable N oldNode;
    private N resultNode;
    private MergeResultTypeEnum resultType;
}