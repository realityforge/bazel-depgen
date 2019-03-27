package org.realityforge.bazel.depgen;

import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

final class OmitNullRepresenter
  extends Representer
{
  @Override
  protected NodeTuple representJavaBeanProperty( final Object javaBean,
                                                 final Property property,
                                                 final Object propertyValue,
                                                 final Tag customTag )
  {
    // if value of property is null, ignore it.
    return null == propertyValue ?
           null :
           super.representJavaBeanProperty( javaBean, property, propertyValue, customTag );
  }
}
