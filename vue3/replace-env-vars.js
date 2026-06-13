const fs = require('fs');
const path = require('path');

// 要搜索的目录
const searchDir = path.join(__dirname, 'src');

// 要替换的内容
const replacements = [
  {
    pattern: /process\.env\.VUE_APP_BASE_API/g,
    replacement: 'import.meta.env.VITE_BASE_API'
  },
  {
    pattern: /const baseAPI = process\.env\.VUE_APP_BASE_API \|\| "\/api";/g,
    replacement: 'const baseAPI = import.meta.env.VITE_BASE_API || "/api";'
  },
  {
    pattern: /const baseAPI = process\.env\.VUE_APP_BASE_API \|\| '\/api'/g,
    replacement: 'const baseAPI = import.meta.env.VITE_BASE_API || \'/api\''
  }
];

// 要处理的文件扩展名
const extensions = ['.vue', '.js', '.ts', '.jsx', '.tsx'];

// 统计信息
let filesProcessed = 0;
let filesModified = 0;
let replacementsCount = 0;

// 递归遍历目录
function traverseDirectory(directory) {
  const files = fs.readdirSync(directory);
  
  for (const file of files) {
    const fullPath = path.join(directory, file);
    const stat = fs.statSync(fullPath);
    
    if (stat.isDirectory()) {
      // 递归处理子目录
      traverseDirectory(fullPath);
    } else if (stat.isFile() && extensions.includes(path.extname(fullPath))) {
      // 处理文件
      processFile(fullPath);
    }
  }
}

// 处理单个文件
function processFile(filePath) {
  filesProcessed++;
  
  try {
    let content = fs.readFileSync(filePath, 'utf8');
    const originalContent = content;
    
    // 应用所有替换
    let modified = false;
    for (const { pattern, replacement } of replacements) {
      if (pattern.test(content)) {
        content = content.replace(pattern, replacement);
        modified = true;
        
        // 计算替换次数
        const matches = originalContent.match(pattern);
        if (matches) {
          replacementsCount += matches.length;
        }
      }
    }
    
    // 如果内容有变化，写回文件
    if (modified) {
      fs.writeFileSync(filePath, content, 'utf8');
      filesModified++;
      console.log(`Modified: ${filePath}`);
    }
  } catch (error) {
    console.error(`Error processing ${filePath}: ${error.message}`);
  }
}

// 开始处理
console.log('Starting environment variable replacement...');
console.log(`Searching in: ${searchDir}`);
console.log('Replacing environment variable references...');

traverseDirectory(searchDir);

console.log('\nSummary:');
console.log(`Files processed: ${filesProcessed}`);
console.log(`Files modified: ${filesModified}`);
console.log(`Replacements made: ${replacementsCount}`);
console.log('Done!'); 