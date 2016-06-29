package edu.toronto.cs.se.ci.ML;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;

import edu.toronto.cs.se.ci.Contracts;
import edu.toronto.cs.se.ci.Source;
import edu.toronto.cs.se.ci.UnknownException;
import edu.toronto.cs.se.ci.budget.Expenditure;
import edu.toronto.cs.se.ci.budget.basic.Dollars;
import edu.toronto.cs.se.ci.budget.basic.Time;
import edu.toronto.cs.se.ci.data.Opinion;
import edu.toronto.cs.se.ci.machineLearning.MLBasicSource;
import edu.toronto.cs.se.ci.machineLearning.MLToCIContract;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MLToCIContractTest extends TestCase {
	RetA a;
	RetB b;
	RetC c;
	RetSame s;

	public MLToCIContractTest() {
		super("MLToCIContractTest");
	}

	public static Test suite() {
		return new TestSuite(MLToCIContractTest.class);
	}

	@Override
	public void setUp() {
		a = new RetA();
		b = new RetB();
		c = new RetC();
		s = new RetSame();
		Contracts.clear();
	}

	public void testSingleSource() throws Exception {
		Contracts.register(a);
		MLToCIContract<Object, String> t1 = new MLToCIContract<Object, String>(test1.class);
		List<Source<Object, String, Void>> resList = t1.provide();
		assertEquals(1, resList.size());
		Source<Object, String, Void> res = resList.get(0);
		assertSame(res, res.provide().get(0));
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetA", res.getName());
		assertSame(res, res.provideML().get(0));
		assertEquals(0, res.getCost(null).length);
		Opinion<String, Void> opinion = res.getOpinion(null);
		assertNull(opinion.getTrust());
		assertEquals("A", opinion.getValue());
		// TODO: Determine if it would be better to instead make MLToCIContract
		// give the original source to the opinion
		assertNull(opinion.getSource());
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetA", opinion.getName());
	}

	public void testSingleIntelligentSource() throws Exception {
		Contracts.register(s);
		MLToCIContract<Object, String> t1 = new MLToCIContract<Object, String>(test1.class);
		List<Source<Object, String, Void>> resList = t1.provide();
		assertEquals(1, resList.size());
		Source<Object, String, Void> res = resList.get(0);
		assertSame(res, res.provide().get(0));
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetSame", res.getName());
		assertSame(res, res.provideML().get(0));
		assertNull(res.getCost(null));
		Opinion<String, Void> opinion = res.getOpinion("Dumdedumdumdah");
		assertNull(opinion.getTrust());
		assertEquals("rsp: Dumdedumdumdah", opinion.getValue());
		assertNull(opinion.getSource());
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetSame", opinion.getName());
	}

	public void testAllSources() throws Exception{
		Contracts.register(a);
		Contracts.register(b);
		Contracts.register(c);
		Contracts.register(s);

		MLToCIContract<Object, String> t1 = new MLToCIContract<Object, String>(test1.class);
		List<Source<Object, String, Void>> resList = t1.provide();
		assertEquals(4, resList.size());
		
		Source<Object, String, Void> res = resList.get(0);
		assertSame(res, res.provide().get(0));
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetA", res.getName());
		assertSame(res, res.provideML().get(0));
		assertEquals(0, res.getCost(null).length);
		Opinion<String, Void> opinion = res.getOpinion(null);
		assertNull(opinion.getTrust());
		assertEquals("A", opinion.getValue());
		assertNull(opinion.getSource());
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetA", opinion.getName());
		
		res = resList.get(1);
		assertSame(res, res.provide().get(0));
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetB", res.getName());
		assertSame(res, res.provideML().get(0));
		assertEquals(1, res.getCost(null).length); //TODO add check that it is Dollars
		opinion = res.getOpinion(null);
		assertNull(opinion.getTrust());
		assertEquals("B", opinion.getValue());
		assertNull(opinion.getSource());
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetB", opinion.getName());
		
		res = resList.get(2);
		assertSame(res, res.provide().get(0));
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetC", res.getName());
		assertSame(res, res.provideML().get(0));
		assertEquals(2, res.getCost(5).length); //TODO add check that it is Dollars and Time
		opinion = res.getOpinion(null);
		assertNull(opinion.getTrust());
		assertEquals("C", opinion.getValue());
		assertNull(opinion.getSource());
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetC", opinion.getName());
		
		res = resList.get(3);
		assertSame(res, res.provide().get(0));
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetSame", res.getName());
		assertSame(res, res.provideML().get(0));
		assertNull(res.getCost("quantum"));
		opinion = res.getOpinion(null);
		assertNull(opinion.getTrust());
		assertEquals("rsp: null", opinion.getValue());
		assertNull(opinion.getSource());
		assertEquals("edu.toronto.cs.se.ci.ML.MLToCIContractTest$RetSame", opinion.getName());
	}
	
	public void testNoSource() throws Exception {
		MLToCIContract<Object, String> t1 = new MLToCIContract<Object, String>(test1.class);
		List<Source<Object, String, Void>> resList = t1.provide();
		assertEquals(0, resList.size());
	}

	public class RetA extends MLBasicSource<Object, String> implements test1 {
		@Override
		public String getResponse(Object input) throws UnknownException {
			return "A";
		}

		@Override
		public Expenditure[] getCost(Object args) throws Exception {
			return new Expenditure[] {};
		}
	}

	public class RetB extends Source<Object, String, Integer> implements test1 {

		@Override
		public Expenditure[] getCost(Object args) throws Exception {
			return new Expenditure[] { new Dollars(new BigDecimal(5)) };
		}

		@Override
		public Opinion<String, Integer> getOpinion(Object args) throws UnknownException {
			return new Opinion<String, Integer>("B", 25, this);
		}

		@Override
		public Integer getTrust(Object args, Optional<String> value) {
			return 15;
		}
	}

	public class RetC extends MLBasicSource<Object, String> implements test1 {

		@Override
		public String getResponse(Object input) throws UnknownException {
			return "C";
		}

		@Override
		public Expenditure[] getCost(Object args) throws Exception {
			return new Expenditure[] { new Time(1, TimeUnit.MILLISECONDS), new Dollars(new BigDecimal(99)) };
		}

	}

	public class RetSame extends MLBasicSource<Object, String> implements test1 {

		@Override
		public String getResponse(Object input) throws UnknownException {
			return "rsp: " + input;
		}

		@Override
		public Expenditure[] getCost(Object args) throws Exception {
			return null;
		}
	}
}
