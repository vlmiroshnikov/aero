package org.aero.common
import com.aerospike.client.Key
import org.aero.Schema

object KeyBuilder {
  def make[K](key: K)(implicit schema: Schema, kw: KeyEncoder[K]): Key = {
    new Key(schema.namespace, schema.set, kw.encoder(key))
  }
}
