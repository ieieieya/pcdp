package chapter03.threadsynchronizationtools.demo05.phaser_tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

public class FileSearch implements Runnable {

	/**
	 * 初始目录
	 */
	private String initPath;

	/**
	 * 文件的后缀名
	 */
	private String end;

	/**
	 * 存储要查找的符合后缀名的文件的全路径
	 */
	private List<String> results;

	/**
	 * 控制FileSearch对象的执行.它们的执行将分成3个步骤：
	 * 1. 在指定的目录和子目录中查找指定扩展名的的文件
	 * 2. 过滤结果.只想得到今天修改的文件
	 * 3. 打印结果
	 */
	private Phaser phaser;

	public FileSearch(String initPath, String end, Phaser phaser) {
		this.initPath = initPath;
		this.end = end;
		this.phaser = phaser;
		results = new ArrayList<>();
	}

	@Override
	public void run() {
		// 等待所有的FileSearah对象的创建,将会阻塞
		// 比如要是Main中创建的system线程先启动,那么要等待apps和documents线程创建结束
		phaser.arriveAndAwaitAdvance();

		System.out.printf("%s: Starting.\n", Thread.currentThread().getName());

		// 第一步：查找文件
		File file = new File(initPath);
		if (file.isDirectory()) {
			directoryProcess(file);
		}

		// 如果列表为空,在phaser撤销然后结束
		if (!checkResults()) {
			return;
		}

		// 第二步：过滤结果
		filterResults();

		// 如果列表为空,在phaser撤销然后结束
		if (!checkResults()) {
			return;
		}

		// 第三步, 打印信息
		showInfo();
		phaser.arriveAndDeregister();
		System.out.printf("%s: Work completed.\n", Thread.currentThread().getName());
	}
	
	/**
	 * 第一阶段：处理目录
	 */
	private void directoryProcess(File file) {
		File[] list = file.listFiles();
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				if (list[i].isDirectory()) {
					// 目录, 递归调用
					directoryProcess(list[i]);
				} else {
					// 文件
					fileProcess(list[i]);
				}
			}
		}
	}

	private void fileProcess(File file) {
		if (file.getName().endsWith(end)) {
			// 将文件的全路径加入到列表中
			results.add(file.getAbsolutePath());
		}
	}

	
	/**
	 * 第二阶段：过滤文件. 过滤第一阶段包含的列表.
	 */
	private void filterResults() {
		List<String> newResults = new ArrayList<>();
		long actualDate = new Date().getTime();
		for (int i = 0; i < results.size(); i++) {
			File file = new File(results.get(i));
			long fileDate = file.lastModified();	// 文件的最后修改时间

			// 不超过24小时,加入到列表中
			if (actualDate - fileDate < TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)) {
				newResults.add(results.get(i));
			}
		}

		results = newResults;
	}
	
	
	/**
	 * 第三阶段：打印信息
	 */
	private void showInfo() {
		for (int i = 0; i < results.size(); i++) {
			File file = new File(results.get(i));
			System.out.printf("%s: %s\n", Thread.currentThread().getName(), file.getAbsolutePath());
		}
		
		// 等待所有在phaser中注册的FileSearch对象的结束
		phaser.arriveAndAwaitAdvance();
	}

	
	/**
	 * 检查结果
	 */
	private boolean checkResults() {
		if (results.isEmpty()) {
			System.out.printf("%s: Phase %d: 0 results.\n", Thread.currentThread().getName(), phaser.getPhase());
			System.out.printf("%s: Phase %d: End.\n", Thread.currentThread().getName(), phaser.getPhase());
			// 没有要查找的文件.撤销Phaser
			// 提醒Phaser这个线程已经结束了实际的阶段,它将离开phased的操作
			phaser.arriveAndDeregister();
			return false;
		} else {
			System.out.printf("%s: Phase %d: %d results.\n", Thread.currentThread().getName(), phaser.getPhase(),
					results.size());

			// 此阶段结束.等待继续执行下一个phase
			// 提醒Phaser这个线程已经结束了实际的阶段,它将阻塞直到在这个阶段的操作的所有参与的线程完成实际的阶段
			phaser.arriveAndAwaitAdvance();
			return true;
		}
	}

}
