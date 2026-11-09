package com.serratocreations.phovo.website.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.ElementBuilder
import org.jetbrains.compose.web.dom.TagElement
import org.w3c.dom.HTMLElement

// Elements Compose HTML doesn't ship a composable for.

private val details = ElementBuilder.createBuilder<HTMLElement>("details")
private val summary = ElementBuilder.createBuilder<HTMLElement>("summary")
private val strong = ElementBuilder.createBuilder<HTMLElement>("strong")
private val figure = ElementBuilder.createBuilder<HTMLElement>("figure")
private val figcaption = ElementBuilder.createBuilder<HTMLElement>("figcaption")
private val time = ElementBuilder.createBuilder<HTMLElement>("time")

@Composable
fun Details(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) =
    TagElement(details, attrs, content)

@Composable
fun Summary(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) =
    TagElement(summary, attrs, content)

@Composable
fun Strong(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) =
    TagElement(strong, attrs, content)

@Composable
fun Figure(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) =
    TagElement(figure, attrs, content)

@Composable
fun Figcaption(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) =
    TagElement(figcaption, attrs, content)

@Composable
fun Time(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) =
    TagElement(time, attrs, content)
