package cn.sumile.SumileJson;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author <a href="http://sumile.cn">sumile</a>
 * @version 0.0000001
 * @Time 2015��10��21��17:55:54
 *  �������뷴�������� wudkj@163.com������վ��������
 */
public class SumileJson {
	private static boolean showError = true;

	public static void showException(String e) {
		if (showError) {
			throw new RuntimeException(e);
		}
	}

	public static <T> Object parse(String str, Class<T> cls) {
		ArrayList<KeyValuePare> listField = getAllTypeAndFields(cls);
		ArrayList<KeyValuePare> listMethod = getAllMethods(cls);
		Object object = null;
		try {
			object = cls.newInstance();
		} catch (InstantiationException e) {
			showException("��" + cls.getName() + "����ʵ������������Ϊ����һ���ӿڻ��߳����ࡣ");
		} catch (IllegalAccessException e) {
			showException("��" + cls.getName() + "�����η����޸�Ϊpublic");
		}
		JSONObject jsobj = null;
		try {
			jsobj = new JSONObject(str);
		} catch (JSONException e) {
			showException("�ַ���<" + str + ">����ת��Ϊһ��JSONObject,�������ǲ�����{}��ס�ġ�");
		}
		for (int i = 0; i < listField.size(); i++) {
			if (listField.get(i).getKey().toString().contains("java.util.ArrayList")) {
				// ��ArrayList�Ļ���Ҫת��Ϊһ������
				JSONArray array = null;
				try {
					array = jsobj.getJSONArray(listField.get(i).getValue());
				} catch (JSONException e) {
					showException("�ַ���<" + listField.get(i).getValue() + ">����һ��JSONArray,�������Ƿ�����[]��ס�ġ�");
				}
				Class c = null;
				c = getTurelyClass(listField.get(i).getKey().toString());
				if (c == null) {
					return null;
				}
				ArrayList<T> list = null;
				list = parser_Array(array, c);
				if (containsMethods(listMethod, "set" + toUpperCaseFirstOne(listField.get(i).getValue()))) {
					// ���json�ַ����е�ֵ
					Method method = null;
					try {
						method = cls.getDeclaredMethod("set" + toUpperCaseFirstOne(listField.get(i).getValue()), ArrayList.class);
					} catch (NoSuchMethodException e) {
						showException("����" + "set" + toUpperCaseFirstOne(listField.get(i).getValue()) + "�����Ƿ���ڡ�");
					} catch (SecurityException e) {
						showException(e.toString());
					}
					setData(object, method, list);
				}
			} else {
				// �ǵ��������ԣ�ֱ������ֵ
				if (containsMethods(listMethod, "set" + toUpperCaseFirstOne(listField.get(i).getValue()))) {
					// ���json�ַ����е�ֵ
					String jsData = null;
					try {
						jsData = jsobj.getString(listField.get(i).getValue());
					} catch (JSONException e) {
						showException("���<" + listField.get(i).getValue() + ">ʱ��������JSON�ı�");
					}
					Method method = null;
					try {
						method = cls.getDeclaredMethod("set" + toUpperCaseFirstOne(listField.get(i).getValue()), String.class);
					} catch (NoSuchMethodException e) {
						showException("����" + "set" + toUpperCaseFirstOne(listField.get(i).getValue()) + "�����Ƿ���ڡ�");
					} catch (SecurityException e) {
						showException(e.toString());
					}
					setData(object, method, jsData);
				}
			}
		}
		return (T) object;
	}

	private static <T> ArrayList<T> parser_Array(JSONArray array, Class<T> cls) {
		ArrayList<T> list = new ArrayList<T>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject jsobj = null;
			try {
				jsobj = (JSONObject) array.get(i);
			} catch (JSONException e) {
				showException(array.toString() + "�е�����һ���һ��JSONObject");
			}
			Object obj = parse(jsobj.toString(), cls);
			list.add((T) obj);
		}
		return list;
	}

	private static Class<?> getTurelyClass(String type) {
		type = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
		Class c = null;
		try {
			c = Class.forName(type);
		} catch (ClassNotFoundException e) {
			showException("��" + type + "�����ڣ����ܱ�ʵ����");
		}
		return c;
	}

	/**
	 * �����Ƿ������ǰ�ķ���
	 */
	private static boolean containsMethods(ArrayList<KeyValuePare> list, String methodName) {
		for (int i = 0; i < list.size(); i++) {
			if (methodName.equals(list.get(i).getValue())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * ���÷���������ֵ
	 * @param obj	newInstance֮��Ķ���
	 * @param method	����
	 * @param data	Ҫ���õ�����
	 */
	private static void setData(Object obj, Method method, Object data) {
		try {
			method.invoke(obj, data);
		} catch (Exception e) {
			throw new RuntimeException("����:" + method.toString() + "��������ֵ:" + data + "ʱ����");
		}
	}

	/**
	 * ����������еķ���������ֵ����������Ĳ����ĵ�һ���Լ�����������
	 */
	private static ArrayList<KeyValuePare> getAllMethods(Class cls) {
		ArrayList<KeyValuePare> pare = new ArrayList<KeyValuePare>();
		Method[] methods = cls.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			String methodName = methods[i].getName();
			if (methodName.contains("set")) {
				Type[] types = methods[i].getGenericParameterTypes();
				KeyValuePare k = new KeyValuePare();
				k.setKey(types[0]);
				k.setValue(methodName);
				pare.add(k);
			}
		}
		return pare;
	}

	/**
	 * ������е��ڲ�����
	 * @return 
	 */
	private static Field[] baseGetAllFields(Class cls) {
		Field[] fs = cls.getDeclaredFields();
		return fs;
	}

	/**
	 * ����������Ե�ֵ�Լ�����
	 */
	private static ArrayList<KeyValuePare> getAllTypeAndFields(Class cls) {
		ArrayList<KeyValuePare> pare = new ArrayList<>();
		Field[] fs = baseGetAllFields(cls);
		for (int i = 0; i < fs.length; i++) {
			Field f = fs[i];
			f.setAccessible(true); // ����Щ�����ǿ��Է��ʵ�
			// ������Ե�����
			Type fileType = f.getGenericType();
			// ������Ե�����
			String fileName = f.getName();
			KeyValuePare k = new KeyValuePare();
			k.setKey(fileType);
			k.setValue(fileName);
			pare.add(k);
		}
		return pare;
	}

	public static String toUpperCaseFirstOne(String s) {
		if (Character.isUpperCase(s.charAt(0)))
			return s;
		else
			return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
	}

	public static class KeyValuePare {
		private Type key;
		private String value;

		public Type getKey() {
			return key;
		}

		public void setKey(Type key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}
}
