package com.ardentex.spark.hiveudf;

import java.util.ArrayList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDFUtils;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.io.parquet.serde.ArrayWritableObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorConverters;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorConverters.Converter;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
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
  private ObjectInspector returnOI;
  private PrimitiveObjectInspector inputOI;
  private PrimitiveObjectInspector outputOI;
  private Tagger tagger = null;
  private Model model = null;
  private Lattice lattice = null;

  @Override
  public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
    // This UDF accepts one argument
    assert (arguments.length == 1);
    // The first argument is a primitive type
    assert(arguments[0].getCategory() == Category.PRIMITIVE);
    this.inputOI  = (PrimitiveObjectInspector)arguments[0];

    GenericUDFUtils.ReturnObjectInspectorResolver returnOIResolver = new GenericUDFUtils.ReturnObjectInspectorResolver(true);
    returnOI = returnOIResolver.get(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
    System.out.println("java.library.path="+System.getProperty("java.library.path"));
    Tagger tagger2 = null;
    if (this.tagger == null) {
        System.err.println("beberexha");
        try {
            System.load("/usr/lib/hadoop/lib/native/libMeCab.so"); // refrain from using loadLibrary for some serious reasons...
            System.err.println("tailor swift");
            System.err.println(MeCab.VERSION);
            System.err.println("clapton");
            try {
                // tagger2 = new Tagger("-Ochasen -d /home/hadoop/spark-hive-udf-mecab/mecab/lib/mecab/dic/");
                //// tagger2 = new Tagger();
                this.model = new Model();
                this.tagger = model.createTagger();
                //this.lattice = model.createLattice();

            } catch (java.lang.Exception e) {
                System.err.println("catch RuntimeError:");
                e.printStackTrace();
                tagger2 = null;
            }
            System.err.println("And you've done.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Cannot load the example native code.\nMake sure your LD_LIBRARY_PATH contains \'.\'\n" + e);
            System.err.println("*** I would like to stop this program with exit.\nbut I can not...");
            tagger2 = null;
        }
        //System.err.println("tagger2="+tagger2.hashCode());
        this.tagger = tagger2;
    }
    return ObjectInspectorFactory.getStandardListObjectInspector(returnOI);
  }

  @Override
  public Object evaluate(DeferredObject[] arguments) throws HiveException {
    // System.err.println("enter evaluate");
    /* We only support STRING type */
    assert(this.inputOI.getPrimitiveCategory() == PrimitiveCategory.STRING);
    // System.err.println("after assert");

    /* And we'll return a type int, so let's return the corresponding object inspector */
    this.outputOI = PrimitiveObjectInspectorFactory.writableStringObjectInspector;
    // System.err.println("after outputOI");

    ret.clear();
    Object oin = arguments[0].get();
    // System.err.println("after oin:"+oin.toString());

    if (oin == null) return null;
    String value = (String)this.inputOI.getPrimitiveJavaObject(oin);
    System.err.println(value); 
    ArrayList<Object> words = new ArrayList<Object>();
    Node node = this.tagger.parseToNode(value);
    for (;node != null; node = node.getNext()) {
        StringBuffer sb = new StringBuffer(node.getSurface());
        String w = sb.toString();
        if (w.length() > 0) {
            words.add(w);
        }
    }
    //this.tagger.parse(lattice);
    //this.lattice.set_sentence(value);
    // if (this.tagger.parse(lattice)) {
    //    System.out.println(lattice.toString());
    //    for (node = lattice.bos_node(); node != null; node = node.getNext()) {
    //       StringBuffer sb = new StringBuffer(node.getSurface());
    //       String w = sb.toString();
    //       if (w.length() > 0) {
    //          words.add(w);
    //       }
    //    }
    // }
    // for (;node != null; node = node.getNext()) {
    //     StringBuffer sb = new StringBuffer(node.getSurface());
    //     String w = sb.toString();
    //     if (w.length() > 0) {
    //         words.add(w);
    //     }
    // }
    // ret = words;
    // System.err.println("success! "+ret.toString()); 
    ret = words;
    return ret;
  }

  @Override
  public String getDisplayString(String[] children) {
    return getStandardDisplayString("array", children, ",");
  }
}

