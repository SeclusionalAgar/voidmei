package prog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import parser.hudMsg;
import parser.mapInfo;
import parser.mapObj;

public class otherService implements Runnable {

	String sMapInfo;
	String sMapObj;
	String shudMsg;

	controller xc;
	public mapInfo mapi;
	public mapObj mapo;

	public float distance;
	public float enemyspeed;
	public double AOT;
	public double AZI;

	public int enemycount;
	public int friendcount;

	public int dislmt;
	float pX;
	float pY;
	long SpeedCheckMili;

	hudMsg msg;
	int lastEvt;
	int lastDmg;

	public volatile boolean isRun;
	boolean isgetMsg;
	boolean isgetmapObj;
	boolean isOverheat;
	boolean hisOverheat;
	int check;

	public double angleToclock(double angle) {
		double temp;
		temp = 12 + angle / 30.0f;
		if (temp >= 12)
			temp = temp - 12;
		return temp;
	}

	public double dxdyToangle(double dx, double dy) {
		double tems;
		tems = Math.atan(dy / dx) * 180 / Math.PI;
		if (dy >= 0 && dx <= 0) {
			tems = 180 + tems;
		}
		if (dy <= 0 && dx <= 0) {
			tems = 180 + tems;
		}
		if (dy <= 0 && dx >= 0) {
			tems=360+tems;
		}
		return tems;
	}

	public String sendGet(String host, int port, String path) throws IOException {

		String result = "";
		Socket socket = new Socket();
		SocketAddress dest = new InetSocketAddress(host, port);
		socket.connect(dest);
		OutputStreamWriter streamWriter = new OutputStreamWriter(socket.getOutputStream());
		BufferedWriter bufferedWriter = new BufferedWriter(streamWriter);

		bufferedWriter.write("GET " + path + " HTTP/1.1\r\n");
		bufferedWriter.write("Host: " + host + "\r\n");
		bufferedWriter.write(app.httpHeader);
		bufferedWriter.write("\r\n");
		bufferedWriter.flush();

		BufferedInputStream streamReader = new BufferedInputStream(socket.getInputStream());

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, "utf-8"));

		String line = null;

		bufferedReader.ready();
		bufferedReader.readLine();

		bufferedReader.readLine();
		bufferedReader.readLine();
		bufferedReader.readLine();
		bufferedReader.readLine();
		bufferedReader.readLine();
		// System.out.println(System.currentTimeMillis()-testCheckMili);
		StringBuilder contentBuf = new StringBuilder();
		while ((line = bufferedReader.readLine()) != null) {
			contentBuf.append(line);
		}
		result = contentBuf.toString();

		bufferedReader.close();
		bufferedWriter.close();
		socket.close();
		return result;
	}

	public void init(controller c) {
		isRun = true;
		xc = c;
		pX = 0;
		pY = 0;
		lastEvt = 0;
		lastDmg = 0;
		lastEvt = xc.lastEvt;
		lastDmg = xc.lastDmg;
		isgetMsg = true;
		isgetmapObj = true;
		isOverheat = false;
		hisOverheat = false;
		//
		dislmt = 1200;
		SpeedCheckMili = System.currentTimeMillis();
		mapi = new mapInfo();
		mapi.init();
		if (isgetmapObj) {
			mapo = new mapObj();
			mapo.init();
		}
		if (isgetMsg) {
			msg = new hudMsg();
			msg.init();
		}

		// ��ʼ����ͼ���ã�����ߴ�
		try {
			sMapInfo = sendGet("127.0.0.1", 8111, "/map_info.json");
			mapi.update(sMapInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void calculate() {
		// ����ѡ��Ŀ���ˮƽ��Ծ��뼰�ٶȼ�AOT
		double pys;
		double eys;
		if (mapo.slc.type != "") {

			distance = (float) Math.sqrt((mapo.slc.x - mapo.pla.x) * (mapo.slc.x - mapo.pla.x) * mapi.cmapmaxsizeX
					* mapi.cmapmaxsizeX
					+ (mapo.slc.y - mapo.pla.y) * (mapo.slc.y - mapo.pla.y) * mapi.cmapmaxsizeY * mapi.cmapmaxsizeY);
			// System.out.println(distance);

			if (mapo.slc.dx != 0 && distance < dislmt) {
				enemycount++;
			}

			enemyspeed = (float) (Math
					.sqrt(((mapo.slc.x - pX) * mapi.cmapmaxsizeX) * ((mapo.slc.x - pX) * mapi.cmapmaxsizeX)
							+ ((mapo.slc.y - pY) * mapi.cmapmaxsizeX) * ((mapo.slc.y - pY) * mapi.cmapmaxsizeY))
					* 1000 / (System.currentTimeMillis() - SpeedCheckMili));
			SpeedCheckMili = System.currentTimeMillis();
			pys = dxdyToangle(mapo.pla.dx, mapo.pla.dy);
			eys = dxdyToangle(mapo.slc.dx, mapo.slc.dy);
			AOT = Math.abs(pys - eys);
			if(AOT>180)AOT=360-AOT;
			// System.out.println(enemyspeed*3.6 );
			AZI = angleToclock(dxdyToangle(mapo.slc.x - mapo.pla.x, mapo.slc.y - mapo.pla.y) - pys);
			// System.out.println(mapo.slc.dx);
			// System.out.println(enemycount);
		}

		// ͳ����Χ�л������ѻ���
		int i;
		for (i = 0; i < mapo.movcur; i++) {
			double sdistance = Math.sqrt(
					(mapo.mov[i].x - mapo.pla.x) * (mapo.mov[i].x - mapo.pla.x) * mapi.cmapmaxsizeX * mapi.cmapmaxsizeX
							+ (mapo.mov[i].y - mapo.pla.y) * (mapo.mov[i].y - mapo.mov[i].y) * mapi.cmapmaxsizeY
									* mapi.cmapmaxsizeY);
			if (sdistance < dislmt && sdistance < mapo.mov[i].distance) {
				if (mapo.mov[i].colorg.getBlue() > 200 || mapo.mov[i].colorg.getGreen() > 200) {
					friendcount++;
					// System.out.println((mapo.mov[i].type+"�Ѿ�"+i+"����"+sdistance));
				}
				if (mapo.mov[i].colorg.getRed() > 200) {
					enemycount++;
				}
			}
			mapo.mov[i].distance = sdistance;
		}

		// System.out.println("��Χ�ѻ���" + friendcount + " ��Χ�л���" + enemycount);
	}

	public void close() {
		this.isRun = false;
	}

	public void judgeOverheat() {
		// ����
		if (!hisOverheat && isOverheat) {
			hisOverheat = true;
			// System.out.println("�򿪹��ȼ�ʱ��");
			check = 3;// ���μ��
			xc.startOverheatTime();
		}
		// ���¹���ʱ��
		if (hisOverheat && isOverheat) {
			// System.out.println("���¹���ʱ��");
			check = 3;
			xc.updateOverheatTime();

		}
		// ������ٽ��ܹ�����Ϣ
		if (!isOverheat) {
			if (hisOverheat) {
				if (check == 0) {
					// System.out.println("�ս���ȼ�ʱ��");
					xc.endOverheatTime();
					hisOverheat = false;
					check--;

				} else {
					// System.out.println("�����ȼ�����-1");
					check--;
				}
			}
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (isRun) {
			// 500����ִ��һ��
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// ȡ�õ�ͼ����
			// System.out.println("���ڴ�����ͼ����");
			enemycount = 0;
			friendcount = 0;
			try {
				if (isgetmapObj)
					sMapObj = sendGet("127.0.0.1", 8111, "/map_obj.json");
				if (isgetMsg)
					shudMsg = sendGet("127.0.0.1", 8111, "/hudmsg?lastEvt=" + lastEvt + "&lastDmg=" + lastDmg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			// System.out.println(sMapObj);
			if (isgetmapObj)
				mapo.update(sMapObj);
			if (isgetMsg) {
				lastDmg = msg.update(shudMsg, lastDmg);
				if (msg.dmg.updated) {
					// System.out.println("���ȼ��" + msg.dmg.msg.indexOf("��") +
					// "���߼��" + msg.dmg.msg.indexOf("��"));
					if (msg.dmg.msg.indexOf(language.oSkeyWord1) != -1
							|| msg.dmg.msg.indexOf(language.oSkeyWord2) != -1) {
						isOverheat = true;
						// System.out.println("��⵽���ȱ�־" + isOverheat);
					}
				} else {

					isOverheat = false;
					// System.out.println("��⵽�����ȱ�־" + isOverheat);
				}
			}
			// ������ͼ����

			calculate();
			pX = mapo.slc.x;
			pY = mapo.slc.y;

			// ���HUDMSG��Ϣ��֪ͨ��ҹ���
			judgeOverheat();
			// System.out.println("otherServiceִ����");
		}

	}
}