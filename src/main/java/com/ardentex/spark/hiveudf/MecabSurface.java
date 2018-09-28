package com.ardentex.spark.hiveudf;

import java.util.ArrayList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDFUtils;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorConverters;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorConverters.Converter;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.VoidObjectInspector;

import org.chasen.mecab.MeCab;
import org.chasen.mecab.Tagger;
import org.chasen.mecab.Model;
import org.chasen.mecab.Lattice;
import org.chasen.mecab.Node;

/**
 * MecabSurface.java
 *
 */
@Description(name = "text - japanese sentences.",
    value = "_FUNC_(text) - Array of words of Japanese.")
public class MecabSurface extends GenericUDF {
  private transient Converter[] converters;
  private transient ArrayList<Object> ret = new ArrayList<Object>();
  private Model model;
  private Tagger tagger = null;

  @Override
  public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {

    GenericUDFUtils.ReturnObjectInspectorResolver returnOIResolver = new GenericUDFUtils.ReturnObjectInspectorResolver(true);
    ObjectInspector returnOI = returnOIResolver.get(PrimitiveObjectInspectorFactory.javaStringObjectInspector);

    this.tagger = initialize_mecab();

    return ObjectInspectorFactory.getStandardListObjectInspector(returnOI);
  }

  @Override
  public Object evaluate(DeferredObject[] arguments) throws HiveException {
    ret.clear();
    Object oin = arguments[0].get();
    if (oin instanceof String) {
        ret = this.mecab_surface((String)oin.getValue().toString());
    } else if (oin instanceof Text) {
        ret = (ArrayList<Object>)(this.mecab_surface(oin.getValue().toString());
        ArrayList<Text> words = new ArrayList<Text>();
        for (int i = 0 ; i < ret.length; i++ ) {
            words.add(new Text(ret[i]));
        }
    }
    return ret;
  }

  @Override
  public String getDisplayString(String[] children) {
    return getStandardDisplayString("array", children, ",");
  }

  public Tagger initialize_mecab() {
    Tagger tagger2;
    Boolean boo = true;
    try {
       System.load("libMeCab.so"); // refrain from using loadLibrary for some serious reasons...
       tagger2 = new Tagger("-d /home/hadoop/spark-hive-udf-mecab/mecab-ipadic-neologd/");
    } catch (UnsatisfiedLinkError e) {
       System.err.println("Cannot load the example native code.\nMake sure your LD_LIBRARY_PATH contains \'.\'\n" + e);
       System.err.println("*** I would like to stop this program with exit.\nbut I can not...");
       boo = false;
    }
    return tagger2;
  }

  public ArrayList<String> mecab_surface(String text) {
    System.out.println(tagger.parse(text));
    Node node = tagger.parseToNode(text);
    ArrayList<String> words = new ArrayList<String>();
    for (;node != null; node = node.getNext()) {
        StringBuffer sb = new StringBuffer(node.getSurface());
        words.add(sb.toString());
    }
    System.out.println ("EOS\n");
  }
}
}
