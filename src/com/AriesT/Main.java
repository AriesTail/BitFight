package com.AriesT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;

public class Main {

	final static Integer runtime = 10;

	final static HashMap<String, ValueAndParameter> keyword;
	static {
		keyword = new HashMap<String, ValueAndParameter>() {
			{
				put("do", new ValueAndParameter(0, 1));
				put("set", new ValueAndParameter(1, 2));
				put("get", new ValueAndParameter(2, 1));

				put("if", new ValueAndParameter(100, 3));
				put("endif", new ValueAndParameter(101, 0));

				put("add", new ValueAndParameter(200, 2));
				put("sub", new ValueAndParameter(201, 2));
				put("mul", new ValueAndParameter(202, 2));
				put("div", new ValueAndParameter(203, 2));
				
				put("jump", new ValueAndParameter(300, 1));
			}
		};
	}
	final static String RANDOM = "(RANDOM\\[([0-9]+)\\])";
	final static String RESULT = "(RESULT(_A|_B)\\[(([0-9]+)|(CURRENT-([0-9]+))|([a-zA-Z]+))\\])";
	final static String VARIABLE = "([a-zA-Z]+)";
	final static String NUMBER = "([0-9]+|CURRENT)";
	final static String BOOL = "(0|1)";

	static ArrayList<Function> fountionsA = new ArrayList<>();// 记录读入操作
	static ArrayList<Function> fountionsB = new ArrayList<>();

	static ArrayList<Integer> operationA = new ArrayList<>(runtime);// 记录Bit操作
	static ArrayList<Integer> operationB = new ArrayList<>(runtime);

	static HashMap<String, Integer> memoryA = new HashMap<String, Integer>();// 记录变量
	static HashMap<String, Integer> memoryB = new HashMap<String, Integer>();

	static Integer resultA = 0;// 最后结果
	static Integer resultB = 0;

	public static void main(String[] args) {
		try {
			readfile("resources/随机.txt", fountionsA);
			readfile("resources/永远合作.txt", fountionsB);

			lexicalanalysis();

			syntaxanalysis(fountionsA);
			syntaxanalysis(fountionsB);

			startfunction(runtime);
			
			System.out.println("A:" + resultA + "    B:" + resultB);

		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}

	static void readfile(String filepath, ArrayList<Function> functions) throws Exception {
		File file = new File(filepath);
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new Exception("未找到策略文件：" + filepath);
		}

		String line = bufferedReader.readLine();

		while (line != null) {
			functions.add(new Function(line));
			try {
				line = bufferedReader.readLine();
			} catch (IOException e) {
				bufferedReader.close();
				throw new Exception("读行错误");
			}
		}

		bufferedReader.close();
	}

	static void lexicalanalysis() throws Exception {
		for (Function function : fountionsA) {
			String string = function.getKeyword();
			if (keyword.get(string) == null) {
				throw new Exception("错误关键词:" + string);
			}
		}
		for (Function function : fountionsB) {
			String string = function.getKeyword();
			if (keyword.get(string) == null) {
				System.err.println("错误关键词:" + string);
			}
		}
	}

	static void syntaxanalysis(ArrayList<Function> functions) throws Exception {
		Integer le = 1;
		for (Function function : functions) {
			ValueAndParameter currenttype = keyword.get(function.getKeyword());

			if (currenttype.Parameter != function.paNum) {// 检测参数数
				throw new Exception("错误参数数于第" + le + "行 " + function.getKeyword());
			}

			switch (currenttype.value) {
			case 0:// 返回
				if (!Pattern.matches(BOOL + "|" + VARIABLE + "|" + RESULT, function.getFirstparameter())
						|| Pattern.matches("EQUAL|NOTEQUAL|GREATER|LESS", function.getFirstparameter())) {
					throw new Exception("错误参数数于" + le + "行 " + function.getKeyword());
				}
				break;
			case 1:// 赋值
				if (!Pattern.matches(VARIABLE, function.getFirstparameter())
						|| Pattern.matches("EQUAL|NOTEQUAL|GREATER|LESS", function.getFirstparameter())
						|| !Pattern.matches(NUMBER + "|" + RESULT + "|" + VARIABLE + "|" + RANDOM,
								function.getSecondparameter())) {
					throw new Exception("错误参数数于" + le + "行 " + function.getKeyword());
				}
				break;
			case 2:// 还没用
				break;

			case 100:// 判断
				if (!Pattern.matches(VARIABLE + "|" + NUMBER + "|" + RESULT + "|" + RANDOM,
						function.getFirstparameter())
						|| !Pattern.matches("EQUAL|NOTEQUAL|GREATER|LESS", function.getSecondparameter())
						|| !Pattern.matches(VARIABLE + "|" + NUMBER + "|" + RESULT + "|" + RANDOM, function.getThirdparameter())
						|| Pattern.matches("EQUAL|NOTEQUAL|GREATER|LESS", function.getFirstparameter())
						|| Pattern.matches("EQUAL|NOTEQUAL|GREATER|LESS", function.getThirdparameter())) {
					throw new Exception("错误参数数于" + le + "行 " + function.getKeyword());
				}
				break;
				
			case 200:// 运算
			case 201:
			case 202:
			case 203:
				if (!Pattern.matches(VARIABLE, function.getFirstparameter())
						|| !Pattern.matches(NUMBER, function.getSecondparameter())
						|| Pattern.matches("EQUAL|NOTEQUAL|GREATER|LESS", function.getFirstparameter())) {
					throw new Exception("错误参数数于" + le + "行 " + function.getKeyword());
				}
				break;
				
			case 300://跳转
				if (!Pattern.matches(VARIABLE + "|" + NUMBER, function.getFirstparameter())
						|| Pattern.matches("EQUAL|NOTEQUAL|GREATER|LESS", function.getFirstparameter())) {
					throw new Exception("错误参数数于" + le + "行 " + function.getKeyword());
				}
				break;

			default:
				break;
			}
			le++;
		}
	}

	static void startfunction(int count) throws Exception {
		int current = 0;
		System.out.println("结果：");
		while (count > 0) {
			Function.selfoperation = operationA;// 确定结果
			Function.otheroperation = operationB;
			Integer thisresultA = getresult(current, fountionsA, memoryA);

			Function.selfoperation = operationB;// 确定结果
			Function.otheroperation = operationA;
			Integer thisresultB = getresult(current, fountionsB, memoryB);
			
			if (thisresultA == 0 && thisresultB == 0) {
				System.out.println("A欺骗，B欺骗");
				resultA = resultA + 1;
				resultB = resultB + 1;
			} else if (thisresultA == 0 && thisresultB == 1) {
				System.out.println("A欺骗，B合作");
				resultA = resultA + 5;
			} else if (thisresultA == 1 && thisresultB == 0) {
				System.out.println("A合作，B欺骗");
				resultB = resultB + 5;
			} else if (thisresultA == 1 && thisresultB == 1) {
				System.out.println("A合作，B合作");
				resultA = resultA + 3;
				resultB = resultB + 3;
			} else {
				throw new Exception("操作错误？");
			}
			current++;
			count--;
		}
	}

	static Integer getresult(Integer current, ArrayList<Function> functions, HashMap<String, Integer> memory)
			throws Exception {

		Integer doit = 1;
		Integer le = 1;

		for (int i = 0; i < functions.size(); i++) {
			
			Function function =functions.get(i);
//			System.out.println(function.toString());

			ValueAndParameter currenttype = keyword.get(function.getKeyword());

			switch (currenttype.value) {
			case 0:// 返回
				if (doit > 0) {
					if (Pattern.matches(BOOL, function.getFirstparameter())) {
						Function.selfoperation.add(current, Integer.parseInt(function.getFirstparameter()));
						System.out.println("");
						return Integer.parseInt(function.getFirstparameter());
					} else if (Pattern.matches(VARIABLE, function.getFirstparameter())) {
						if (memory.get(function.getFirstparameter()) == null) {
							throw new Exception("未定义变量：" + le + "行  " + function.getFirstparameter());
						} else if (memory.get(function.getFirstparameter()) != 0
								&& memory.get(function.getFirstparameter()) != 1) {
							throw new Exception("变量值不为0或1：" + le + "行  " + memory.get(function.getFirstparameter()));
						} else {
							Function.selfoperation.add(current, memory.get(function.getFirstparameter()));
							System.out.println("");
							return memory.get(function.getFirstparameter());
						}

					} else if (Pattern.matches(RESULT, function.getFirstparameter())) {
						Function.selfoperation.add(current, dealwithRESULT(current, function, function.getFirstparameter(), memory, GET));
						System.out.println("");
						return dealwithRESULT(current, function, function.getFirstparameter(), memory, GET);
					}
				}
				break;
			case 1:// 赋值
				if (doit > 0) {
					if (Pattern.matches(NUMBER, function.getSecondparameter())) {
						if (function.getSecondparameter().equals("CURRENT")) {
							memory.put(function.getFirstparameter(), current);
						} else {
							memory.put(function.getFirstparameter(), Integer.parseInt(function.getSecondparameter()));
						}

					} else if (Pattern.matches(RESULT, function.getSecondparameter())) {
						dealwithRESULT(current, function, function.getSecondparameter(), memory, SET);

					} else if (Pattern.matches(VARIABLE, function.getSecondparameter())) {
						Integer value = memory.get(function.getSecondparameter());
						if (value == null) {
							throw new Exception("未定义变量：" + le + "行 " + function.getSecondparameter());
						} else {
							memory.put(function.getFirstparameter(), value);
						}

					} else if (Pattern.matches(RANDOM, function.getSecondparameter())) {
						Random random = new Random();
						Integer num = random.nextInt(Integer.parseInt(function.getSecondparameter().split("\\[")[1]
								.substring(0, function.getSecondparameter().split("\\[")[1].length() - 1)));
						memory.put(function.getFirstparameter(), num);
					}
				}
				break;
			case 2:// 还没用
				break;

			case 100:// 判断
				if(doit > 0) {
					Integer firstvalue = null;
					Integer secondvalue = null;

					if (Pattern.matches(NUMBER, function.getFirstparameter())) {// 数字
						if (function.getFirstparameter().equals("CURRENT")) {
							firstvalue = current;
						} else {
							firstvalue = Integer.parseInt(function.getFirstparameter());
						}

					} else if (Pattern.matches(VARIABLE, function.getFirstparameter())) {// 变量
						if (memory.get(function.getFirstparameter()) == null) {
							throw new Exception("未定义变量：" + le + "行 " + function.getFirstparameter());
						} else {
							firstvalue = memory.get(function.getFirstparameter());
						}

					} else if (Pattern.matches(RESULT, function.getFirstparameter())) {// 结果
						firstvalue = dealwithRESULT(current, function, function.getFirstparameter(), memory, GET);

					} else if (Pattern.matches(RANDOM, function.getFirstparameter())) {// 随机数
						Random random = new Random();
						Integer num = random.nextInt(Integer.parseInt(function.getFirstparameter().split("\\[")[1]
								.substring(0, function.getFirstparameter().split("\\[")[1].length() - 1)));
						firstvalue = num;
					}

					if (Pattern.matches(NUMBER, function.getThirdparameter())) {// 数字
						if (function.getThirdparameter().equals("CURRENT")) {
							secondvalue = current;
						} else {
							secondvalue = Integer.parseInt(function.getThirdparameter());
						}

					} else if (Pattern.matches(VARIABLE, function.getThirdparameter())) {// 变量
						if (memory.get(function.getThirdparameter()) == null) {
							throw new Exception("未定义变量：" + le + "行 " + function.getThirdparameter());
						} else {
							secondvalue = memory.get(function.getThirdparameter());
						}

					} else if (Pattern.matches(RESULT, function.getThirdparameter())) {// 结果
						secondvalue = dealwithRESULT(current, function, function.getThirdparameter(), memory, GET);

					} else if (Pattern.matches(RANDOM, function.getThirdparameter())) {// 随机数
						Random random = new Random();
						Integer num = random.nextInt(Integer.parseInt(function.getThirdparameter().split("\\[")[1]
								.substring(0, function.getThirdparameter().split("\\[")[1].length() - 1)));
						secondvalue = num;
					}

					switch (function.getSecondparameter()) {
					case "EQUAL":
						if (firstvalue == secondvalue) {
							// doit++;
						} else {
							doit--;
						}
						break;
					case "NOTEQUAL":
						if (firstvalue != secondvalue) {
							// doit++;
						} else {
							doit--;
						}
						break;
					case "GREATER":
						if (firstvalue > secondvalue) {
							// doit++;
						} else {
							doit--;
						}
						break;
					case "LESS":
						if (firstvalue < secondvalue) {
							// doit++;
						} else {
							doit--;
						}
						break;
					default:
						throw new Exception("错误关键字：" + le + "行 " + function.getSecondparameter());
					}
				} else {//跳过时
					doit--;
				}
				break;

			case 101:// 结束判断
				doit++;
				break;
				
			case 200://计算
			case 201:
			case 202:
			case 203:
				if (doit > 0) {
					Integer value = memory.get(function.getFirstparameter());
					if (value == null) {
						throw new Exception("未定义变量：" + le + "行 " + function.getSecondparameter());
					} else {
						switch (currenttype.value) {
						case 200:
							value += Integer.parseInt(function.getSecondparameter());
							break;
						case 201:
							value -= Integer.parseInt(function.getSecondparameter());
							break;
						case 202:
							value *= Integer.parseInt(function.getSecondparameter());
							break;
						case 203:
							value /= Integer.parseInt(function.getSecondparameter());
							break;
						default:
							break;
						}
						memory.put(function.getFirstparameter(), value);
					}
				}
				break;

			case 300:
				if (doit > 0) {
					int value = -1;
					if (Pattern.matches(NUMBER, function.getFirstparameter())) {// 数字
						if (function.getFirstparameter().equals("CURRENT")) {
							value = current;
						} else {
							value = Integer.parseInt(function.getFirstparameter());
						}

					} else if (Pattern.matches(VARIABLE, function.getFirstparameter())) {// 变量
						if (memory.get(function.getFirstparameter()) == null) {
							throw new Exception("未定义变量：" + le + "行 " + function.getFirstparameter());
						} else {
							value = memory.get(function.getFirstparameter());
						}
					}
					
					if (value >= le) {
						throw new Exception("行数错误: "+function.toString());
					} else {
						le = value - 1;
						value--;// 变为从0开始
						doit = 1;
						i = value - 1;//还要往前一步
					}
				}
				break;

			default:
				throw new Exception("这啥语句啊？");
			}
			
			le++;
		}
		throw new Exception("无返回");
	}

	final static Integer SET = 0;
	final static Integer GET = 1;

	static Integer dealwithRESULT(Integer current, Function function, String content, HashMap<String, Integer> memory,
			Integer type) throws Exception {

		String inside = content.split("\\[")[1].substring(0, content.split("\\[")[1].length() - 1);// 中括号内
		String outside = content.split("\\[")[0];

		if (Pattern.matches(NUMBER, inside)) {// 纯数字
			if (inside.equals("CURRENT") || Integer.parseInt(inside) >= current || Integer.parseInt(inside) < 0) {
				throw new Exception("回合数错误" + inside);
			}
			switch (outside) {
			case "RESULT_A":
				if (type == SET) {
					memory.put(function.getFirstparameter(),
							inside.equals("CURRENT") ? current : Function.selfoperation.get(Integer.parseInt(inside)));
					return 0;
				} else if (type == GET) {
					return operationA.get(
							inside.equals("CURRENT") ? current : Function.selfoperation.get(Integer.parseInt(inside)));
				}
				break;
			case "RESULT_B":
				if (type == SET) {
					memory.put(function.getFirstparameter(),
							inside.equals("CURRENT") ? current : Function.otheroperation.get(Integer.parseInt(inside)));
					return 0;
				} else if (type == GET) {
					return operationB.get(
							inside.equals("CURRENT") ? current : Function.otheroperation.get(Integer.parseInt(inside)));
				}
				break;
			default:
				break;
			}
		} else if (Pattern.matches("CURRENT-" + "([0-9]+)", inside)) {
			Integer num = Integer.parseInt(inside.split("-")[1]);
			if (num > current) {
				throw new Exception("回合数错误:" + inside);
			}
			switch (outside) {
			case "RESULT_A":
				if (type == SET) {
					memory.put(function.getFirstparameter(), Function.selfoperation.get(current - num));
					return 0;
				} else if (type == GET) {
					return Function.selfoperation.get(current - num);
				}
				break;
			case "RESULT_B":
				if (type == SET) {
					memory.put(function.getFirstparameter(), Function.otheroperation.get(current - num));
					return 0;
				} else if (type == GET) {
					return Function.otheroperation.get(current - num);
				}
				break;
			default:
				break;
			}
		} else {//内部是变量
			Integer value = memory.get(inside);
			if (value == null) {
				throw new Exception("未定义变量：" + function.getFirstparameter());
			}
			if (value >= current || value < 0) {
				throw new Exception("回合数错误" + function.toString() + value);
			}
			switch (outside) {
			case "RESULT_A":
				if (type == SET) {
					memory.put(function.getFirstparameter(), Function.selfoperation.get(value));
					return 0;
				} else if (type == GET) {
					return Function.selfoperation.get(value);
				}
				break;
			case "RESULT_B":
				if (type == SET) {
					memory.put(function.getFirstparameter(), Function.otheroperation.get(value));
					return 0;
				} else if (type == GET) {
					return Function.otheroperation.get(value);
				}
				break;
			default:
				break;
			}
		}
		throw new Exception("RESULT错误");
	}

	static class Function {

		@Override
		public String toString() {
			return getKeyword()+' '+getFirstparameter()+' '+getSecondparameter()+' '+getThirdparameter();
		}

		public String getKeyword() {
			return keyword;
		}

		public String getFirstparameter() {
			return firstparameter;
		}

		public String getSecondparameter() {
			return secondparameter;
		}

		public String getThirdparameter() {
			return thirdparameter;
		}

		public Integer getpaNum() {
			return paNum;
		}

		static ArrayList<Integer> selfoperation;
		static ArrayList<Integer> otheroperation;

		String keyword;
		String firstparameter;
		String secondparameter;
		String thirdparameter;

		Integer paNum;

		public Function(String string) {
			String[] words = string.split(" ");
			switch (words.length) {
			case 1:
				keyword = words[0];
				paNum = 0;
				break;
			case 2:
				keyword = words[0];
				paNum = 0;
				firstparameter = words[1];
				paNum = 1;
				break;
			case 3:
				keyword = words[0];
				paNum = 0;
				firstparameter = words[1];
				paNum = 1;
				secondparameter = words[2];
				paNum = 2;
				break;
			case 4:
				keyword = words[0];
				paNum = 0;
				firstparameter = words[1];
				paNum = 1;
				secondparameter = words[2];
				paNum = 2;
				thirdparameter = words[3];
				paNum = 3;
				break;
			default:
				break;
			}
		}
	}

	static class ValueAndParameter {
		Integer value;
		Integer Parameter;

		public ValueAndParameter(Integer value, Integer parameter) {
			super();
			this.value = value;
			Parameter = parameter;
		}

	}

}
