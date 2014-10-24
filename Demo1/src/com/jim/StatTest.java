package com.jim;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class StatTest {

	@Test
	public void Stat() {
		Stat s = new Stat();
		assertEquals(s.ops, 0);
		assertEquals(s.bytes, 0);
		assertEquals(s.deviceName, "");
	}

	@Test
	public void addStat() {
		Stat s = new Stat();
		s.add(new Stat(1,2));
		s.add(new Stat(3,4));
		assertEquals(s, new Stat(4,6));
	}

	@Test
	public void addStatStat() {
		Stat s = Stat.add(new Stat(1,2), new Stat(3,4));
		assertEquals(s, new Stat(4,6));
	}

	@Test
	public void subStat() {
		Stat s = new Stat(4,6);
		s.sub(new Stat(1,2));
		assertEquals(s, new Stat(3,4));
	}

	@Test
	public void subStatStat() {
		Stat s = Stat.sub(new Stat(3,4), new Stat(1,2));
		assertEquals(s, new Stat(2,2));
	}

	@Test
	public void StatStat () {
		Stat s = new Stat(2,4);
		Stat s1 = new Stat(s);
		assertEquals(s,s1);
		assertFalse(s == s1);
	}
	
	
	@Test
	public void times() {
		Stat s = new Stat(4,6);
		s.times(3);
		assertEquals(s, new Stat(12,18));
	}
}
