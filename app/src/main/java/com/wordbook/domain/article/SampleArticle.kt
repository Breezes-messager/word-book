package com.wordbook.domain.article

/**
 * 内置示例文章：没有网、没配 API Key 时也能体验“点击查词”。
 * 里面刻意放了 running / went / decided 这些变形词，用来验证词形还原。
 */
object SampleArticle {
    val content = ArticleContent(
        title = "A Running Start",
        titleCn = "从一个清晨开始",
        paragraphs = listOf(
            "Last week I decided to go running in the park every morning. " +
                "My friend Tom went with me on the first day, and we felt great.",
            "At first, I could only run for ten minutes. My legs hurt, and I wanted to give up. " +
                "Tom told me to breathe slowly and keep going.",
            "After two weeks, things improved. I am sleeping better and thinking more clearly. " +
                "Running has become the best part of my day.",
            "I am not a fast runner yet, but I have learned something important: " +
                "small steps, taken every day, lead to big changes.",
        ),
        occurrences = listOf(
            ArticleOccurrence("run", "running", 0),
            ArticleOccurrence("go", "went", 0),
            ArticleOccurrence("decide", "decided", 0),
            ArticleOccurrence("improve", "improved", 2),
            ArticleOccurrence("think", "thinking", 2),
        ),
        translation = listOf(
            "上周我决定每天早上都去公园跑步。第一天我的朋友汤姆和我一起去了，我们感觉棒极了。",
            "一开始我只能跑十分钟，腿很疼，我想放弃。汤姆叫我要慢慢呼吸，继续坚持。",
            "两周之后，情况好转了。我睡得更好，思路也更清晰。跑步已经成了我一天里最好的时光。",
            "我还不是跑得很快的人，但我学到了一件重要的事：每天迈出一小步，就能带来巨大的改变。",
        ),
    )
}
